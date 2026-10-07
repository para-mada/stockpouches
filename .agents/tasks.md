# StockPouches — Potion Pouch y Totem Pouch

Este backlog pertenece al mod **StockPouches**, no a Forged. Se conserva aquí únicamente como
handoff de diseño porque las ideas se plantearon durante la revisión de Forged.

No implementar estas tareas en el namespace, registros, Mixins, recursos, configuración ni JAR de
Forged. Antes de ejecutar cualquier `SPXX`, abrir el repositorio real de StockPouches y volver a
auditar su versión de Minecraft/Fabric, componentes, convenciones de items, UI, tests, recetas y
política de nesting. Las clases y puntos de inyección mencionados abajo reflejan Minecraft 26.3
observado desde Forged y son hipótesis que StockPouches debe confirmar.

## Intención de diseño

- Potion Pouch comprime varias pociones idénticas; no es una farmacia portátil.
- Totem Pouch comprime reservas; sus tótems internos son inertes.
- Ningún pouch es una mochila genérica ni alimenta automáticamente manos/hotbar.
- “Expedition Gear” puede ser lenguaje compartido con Forged, pero no crea una dependencia técnica
  entre mods.

## Baseline técnico provisional

- `BUNDLE_CONTENTS` persiste y sincroniza contenido, pero calcula peso según el tamaño de stack. Una
  poción o tótem no apilable consume toda la capacidad vanilla, por lo que no representa directamente
  capacidades de 9 o 15.
- `POTION_CONTENTS` y `POTION_DURATION_SCALE`, junto con el item `POTION`, `SPLASH_POTION` o
  `LINGERING_POTION`, distinguen efecto, potencia, duración y forma.
- `ItemStack.isSameItemSameComponents` ofrece una identidad estricta que también conserva nombres y
  componentes de terceros.
- `DEATH_PROTECTION` sólo se activa desde items en las manos. Un pouch sin ese componente mantiene
  inerte el contenido almacenado.
- `SWAP_ITEM_WITH_OFFHAND` se procesa en servidor en `ServerGamePacketListenerImpl`; el gesto `F`
  necesita un callback de StockPouches o, si no existe, un Mixin estrecho y documentado.

## Gate SP-A — Auditoría y decisiones del mod propietario

### SP01 — Auditar el repositorio StockPouches

- **Objetivo:** localizar registro de items/componentes, formato de persistencia, UI, recetas,
  configuración, nesting, sincronización y tests existentes.
- **Resultado esperado:** reemplazar las hipótesis de este documento por rutas y APIs reales del mod.
- **Pensamiento:** Alto.
- **Estado:** completada el 2026-10-06.

#### Resultado de la auditoría SP01

- **Baseline confirmado:** Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3,
  Fabric Loom 1.18-SNAPSHOT (resuelto como 1.18.2), Java 25 y Gradle wrapper 9.7.1. El propio
  proceso de Gradle debe arrancar sobre JDK 25; el toolchain de `build.gradle` no basta si el daemon
  arranca con Java 21.
- **Registro:** `ModDataComponents` registra `stockpouches:contents`; `ModItems` registra los cinco
  pouches actuales y `ModCreativeTabs` los publica tanto en la pestaña propia como en Tools &
  Utilities. `Stockpouches#onInitialize` ordena componentes, items, pestaña, recetas y networking.
- **Persistencia actual:** `PouchContents` es un record inmutable con una lista de hasta 1024
  `PouchEntry` y un índice seleccionado. Cada entrada conserva un `ItemStackTemplate` normalizado a
  count 1 y una cantidad `long`. El componente tiene codec persistente y stream codec de red.
- **Diferencia arquitectónica relevante:** el componente existente admite múltiples variantes y no
  impone capacidad práctica salvo `Long.MAX_VALUE`; Potion/Totem requieren contenido homogéneo y
  capacidad por item. No se debe cambiar implícitamente la semántica de los cinco Stock Pouches ya
  publicados al implementar SP03.
- **UI e interacción:** `StockPouchItem` implementa inserción/extracción mediante clicks de inventario
  y uso en mano; `ClientPouchTooltip` muestra hasta nueve variantes; `PouchMouseActions` selecciona
  por scroll. El único Mixin existente es client-side sobre `AbstractContainerScreen#init` para
  registrar esa acción de ratón.
- **Sincronización:** el componente viaja con el `ItemStack`; la selección usa
  `SelectPouchEntryPayload` y el servidor vuelve a validar slot, tipo de item e índice. No existe
  inventario externo ni estado persistente separado.
- **Recetas:** los cinco pouches usan `PouchRecipe`, un custom recipe de anillo 3x3 alrededor de un
  bundle vanilla vacío. El chequeo explícito de `BUNDLE_CONTENTS` impide borrar el contenido de un
  bundle usado como ingrediente.
- **Nesting actual:** `StockPouchItem#canFitInsideContainerItems` devuelve `false` y la inserción exige
  que el item entrante permita contenedores. Esto impide nesting con la convención vanilla, pero no
  puede garantizar por sí solo compatibilidad con contenedores de terceros que ignoren esa API.
- **Configuración:** no hay framework ni archivo de configuración; las reglas viven en código, tags y
  recetas.
- **Tests:** `PouchContentsTest` contiene siete unit tests sobre conservación, LIFO/selección,
  identidad por componentes, overflow y codec persistente. No hay test de stream codec, tests de
  transacciones/UI, GameTests ni cobertura dedicada server-side. Baseline ejecutado con JDK 25:
  `gradlew test` finaliza correctamente (7/7 tests; sólo avisos de APIs deprecadas).
- **Assets/locales:** existen modelos/items/recetas/tags para cinco pouches, inglés y español de
  España más seis locales regionales de español. Las texturas reutilizan bundles vanilla.

### SP02 — Cerrar contratos de capacidad, identidad y recetas

- **Objetivo:** aprobar capacidades iniciales, crafting/gating y definición de “idéntico”.
- **Recomendación inicial:** 9 pociones, 15 tótems y comparación de item + todos los componentes;
  mantener capacidades como constantes hasta tener playtests.
- **Decisiones:** qué pasa con nombres/custom data; si Totem Pouch acepta sólo el tótem vanilla o
  variantes de otros mods; si un pouch puede entrar en otro contenedor portátil.
- **Dependencia:** SP01.
- **Pensamiento:** Muy alto.
- **Estado:** completada el 2026-10-06; se aprobaron las recomendaciones iniciales al pasar a SP-B.
- **Contrato aprobado:** Potion Pouch 9, Totem Pouch 15, identidad por item y componentes completos,
  Potion Pouch extrae una poción para uso vanilla, sólo `minecraft:totem_of_undying`, `F` desplaza
  primero el offhand al inventario o rechaza atómicamente, nesting prohibido y capacidades constantes.

## Gate SP-B — Componente homogéneo y transacciones

### SP03 — Registrar contenido de tipo único

- **Objetivo:** modelar un prototipo exacto de `ItemStack` con count 1 y una cantidad acotada.
- **Arquitectura propuesta:** data component versionable con codec persistente y de red. La capacidad
  y el predicado de aceptación pertenecen a cada pouch, no al componente, para evitar una mochila
  genérica.
- **Validación:** rechazar cantidad negativa o superior a capacidad; datos inválidos no deben crashear,
  borrar silenciosamente ni duplicar.
- **Dependencias:** SP01–SP02.
- **Pensamiento:** Muy alto.
- **Estado:** completada el 2026-10-06.
- **Implementación:** `SingleTypePouchContents` conserva un `ItemStackTemplate` exacto con count 1,
  cantidad `int` y versión persistente explícita. `stockpouches:single_type_contents` usa el mismo
  codec validado para disco y red. La capacidad se valida contra el item propietario, no se serializa
  en el componente; datos bien formados pero superiores a esa capacidad permanecen intactos y
  bloquean cualquier mutación con resultado `INVALID_CONTENTS`.

### SP04 — Implementar inserción/extracción atómica y anti-nesting

- **Objetivo:** transferir exactamente una unidad conservando la suma pouch + cursor + slot +
  inventario.
- **Trabajo:** resultados explícitos de éxito/fallo/resto, rollback cuando el destino no acepta,
  sincronización server-authoritative y bloqueo de pouches dentro de bundles u otros contenedores
  portátiles según la política aprobada.
- **Tests significativos:** capacidad - 1/capacidad/capacidad + 1, item incompatible, cursor ocupado,
  slot parcial, click repetido y fallo sin cambios parciales.
- **Dependencia:** SP03.
- **Pensamiento:** Muy alto.
- **Estado:** completada el 2026-10-06.
- **Implementación:** `PouchTransactions` mueve exactamente una unidad sobre copias y devuelve
  resultados explícitos sin mutar entradas. `SingleTypePouchItem` aplica ambos lados sólo después de
  validar capacidad, identidad, permisos y destino; si `SlotAccess` rechaza el cambio no actualiza el
  pouch. La sincronización queda en el flujo server-authoritative del menú vanilla.
- **Anti-nesting:** el item base devuelve `false` en `canFitInsideContainerItems`, rechaza ambos tipos
  de pouch de StockPouches como contenido y exige además la convención vanilla para cualquier item
  aceptado.
- **Tests:** ocho casos nuevos cubren codec/componentes, capacidad - 1/capacidad/capacidad + 1,
  rechazo e incompatibilidad, cursor/slot vacío, parcial o lleno, ausencia de mutaciones parciales y
  conservación tras clicks repetidos. Suite total: 16/16; `gradlew build` correcto con JDK 25.

## Gate SP-C — Potion Pouch

### SP05 — Implementar núcleo y restricciones de Potion Pouch

- **Objetivo:** aceptar únicamente colecciones homogéneas de pociones normales, splash o lingering.
- **Invariantes:** distinta forma, efecto, potencia, duración o componentes aprobados es incompatible;
  botellas y flechas no entran sólo por portar `POTION_CONTENTS`; el contenido no participa en brewing
  ni crafting.
- **Dependencias:** SP03–SP04.
- **Pensamiento:** Alto.
- **Estado:** completada el 2026-10-06.
- **Implementación:** `PotionPouchItem` usa capacidad constante 9 y sólo acepta exactamente
  `POTION`, `SPLASH_POTION` o `LINGERING_POTION`. La identidad heredada de SP-B compara item y todos
  los componentes, incluyendo forma, `POTION_CONTENTS`, potencia, duración,
  `POTION_DURATION_SCALE`, nombres y custom data. Botellas y tipped arrows se rechazan aunque porten
  componentes de poción. El pouch no es ingrediente de brewing y su contenido no se expone a
  crafting.

### SP06 — Implementar UX de Potion Pouch

- **Objetivo:** inserción/extracción comprensible sin acceso automático.
- **Recomendación inicial:** clicks de inventario similares a bundle, tooltip de
  prototipo/cantidad/capacidad y extracción a una poción real que después usa la lógica vanilla. No
  beber ni lanzar directamente desde el pouch en el primer incremento.
- **Casos límite:** inventario lleno, cursor ocupado, creative, shift-click, hotbar/offhand y botón
  mantenido.
- **Dependencia:** SP05.
- **Pensamiento:** Alto.
- **Estado:** completada el 2026-10-06.
- **Implementación:** mantiene los clicks atómicos de SP04; el tooltip muestra prototipo y
  cantidad/capacidad. Usar el pouch en cualquier mano mueve una poción real al primer slot libre del
  inventario para que su uso posterior sea totalmente vanilla. Si no existe slot libre o los datos
  son inválidos, rechaza sin cambios; no bebe ni lanza desde el pouch y no alimenta hotbar/offhand.
- **Entrega incremental:** registrado en ambas creative tabs, con modelo provisional basado en el
  bundle morado vanilla y traducciones completas. Receta/progresión y assets finales siguen
  perteneciendo a SP10.
- **Tests:** tres casos dedicados prueban las formas permitidas, el rechazo de botellas/flechas,
  identidad de forma/efecto/potencia/duración/componentes y extracción fiel. Suite total: 19/19;
  `gradlew build` correcto con JDK 25.

## Gate SP-D — Totem Pouch

### SP07 — Implementar núcleo inerte de Totem Pouch

- **Objetivo:** guardar reservas sin que el pouch pueda salvar al jugador.
- **Arquitectura propuesta:** reutilizar SP03–SP04 con capacidad propia; el pouch no recibe
  `DEATH_PROTECTION`, no cuenta como equipable y no contiene lógica de refill.
- **Invariantes:** tótem interno inerte; pouch inerte en ambas manos; ninguna reposición automática
  tras una activación.
- **Dependencias:** SP03–SP04 y decisión de variantes en SP02.
- **Pensamiento:** Alto.

### SP08 — Implementar `F` como extracción intencional

- **Objetivo:** con Totem Pouch en mano principal, cancelar el swap vanilla y mover exactamente un
  tótem al offhand sin colocar allí el pouch.
- **Recomendación para offhand ocupado:** mover primero el item anterior al inventario y completar la
  extracción sólo si toda la transacción cabe; con inventario lleno, rechazar sin cambios.
- **Casos límite:** pouch vacío, espectador, offhand lleno, inventario lleno, pouch en hotbar frente a
  inventario principal, spam de `F` y otro mod que cancele/modifique el swap.
- **Dependencia:** SP07 y aprobación de la semántica del offhand.
- **Pensamiento:** Muy alto.

## Gate SP-E — Persistencia, seguridad y entrega

### SP09 — Probar ciclo de vida y ausencia de duplicación

- **Objetivo:** validar contenido y transacciones antes de aumentar capacidades.
- **Tests significativos:** codec/packet round-trip; incompatibilidad entre pociones; extracción con
  destinos llenos; tótem interno inerte y extraído activo; ausencia de refill; muerte con/sin
  keepInventory, drop/despawn/pickup, cofres, shift-click, creative clone, comandos y nesting.
- **Trabajo manual:** cliente/servidor dedicado, latencia, desconexión durante acción y conflictos de
  swap-hand.
- **Dependencias:** SP05–SP08.
- **Pensamiento:** Muy alto.

### SP10 — Integrar recetas, assets, traducciones y documentación

- **Objetivo:** terminar el ciclo de publicación dentro de StockPouches.
- **Trabajo:** recetas/advancements según su progresión, creative tab, modelos, texturas, tooltips,
  traducciones soportadas, README/changelog y build reproducible del mod propietario.
- **Dependencia:** SP09.
- **Pensamiento:** Medio.

## Invariantes

1. Un pouch contiene un único tipo exacto; no es almacenamiento general.
2. Inserción, extracción y `F` son transacciones atómicas server-side.
3. El contenido almacenado no está equipado ni es accesible automáticamente.
4. Totem Pouch nunca obtiene `DEATH_PROTECTION` y nunca repone manos/hotbar.
5. Los datos viajan con el `ItemStack`; no existe inventario externo susceptible de huérfanos.
6. Ningún pouch puede anidarse de forma que permita ciclos o eluda capacidad.
7. Fallar por capacidad, incompatibilidad o destino lleno no consume ni mueve nada.

## Preguntas que requieren decisión humana

| ID | Decisión | Recomendación inicial |
|---|---|---|
| SP-H01 | Capacidad Potion Pouch | 9. |
| SP-H02 | Capacidad Totem Pouch | 15. |
| SP-H03 | Identidad de poción | Mismo item y mismos componentes completos. |
| SP-H04 | Uso en mano de Potion Pouch | Extraer; uso posterior completamente vanilla. |
| SP-H05 | Variantes de tótem | Sólo `minecraft:totem_of_undying` inicialmente. |
| SP-H06 | Offhand ocupado al pulsar `F` | Transacción a inventario; si no cabe, rechazar. |
| SP-H07 | Nesting | Pouches fuera de cualquier contenedor portátil. |
| SP-H08 | Configuración | Constantes hasta que el playtest justifique config. |

## Orden recomendado

```text
SP01 -> SP02 -> SP03 -> SP04
                    +-> SP05 -> SP06 --+
                    +-> SP07 -> SP08 --+-> SP09 -> SP10
```

Los gates de Potion y Totem pueden revisarse por separado después de estabilizar el componente y las
transacciones. Ninguna tarea `SPXX` depende de clases internas de Forged.
