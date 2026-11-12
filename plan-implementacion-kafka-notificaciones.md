# Plan de implementación: Kafka y notificaciones

## Objetivo

Completar la integración de Kafka solicitada por las evaluaciones EP5 y EP6, conservando los flujos existentes con RabbitMQ. El sistema debe demostrar publicación y consumo de eventos en Kafka, y usar esos eventos para iniciar el procesamiento de notificaciones.

## Plazo y estimación

La fecha de entrega indicada es el **23 de noviembre**. La estimación inicial es de **20–35 horas efectivas**, suponiendo que se trabaje en el alcance necesario para la evaluación, que la infraestructura esté disponible y que no aparezcan problemas importantes de despliegue. Es una estimación orientativa, no una garantía.

La cifra considera tiempo para implementar, integrar, probar y preparar la demostración. Una integración real con proveedores de correo, SMS o notificaciones web podría ampliar el alcance; primero hay que confirmar si la rúbrica exige entregar esos canales o solo demostrar el procesamiento de eventos de notificación.

## Estado actual revisado

* Existe el microservicio `backend/kafka-admin`, que usa Kafka `AdminClient` para crear, listar, modificar y eliminar tópicos, además de consultar grupos de consumidores y offsets.
* La configuración de `backend/kafka-admin/src/main/resources/application.yml` declara los tópicos `pedidos.eventos` e `inventario.alertas`, con factor de réplica 2 y brokers locales en `localhost:9092,localhost:9093`.
* En los servicios de pedidos y productos, los productores y consumidores encontrados usan RabbitMQ. No se encontraron productores ni consumidores Kafka en los microservicios de negocio.
* La infraestructura existente en `infra/` incluye Docker Compose para RabbitMQ; no se encontró un Compose de Kafka y Kafka UI.
* El bootstrap de tópicos registra una advertencia y permite que `kafka-admin` continúe si no logra crearlos. Por ello, que el servicio arranque no demuestra que los tópicos estén listos.
* Las rutas de `kafka-admin` requieren autenticación, pero la configuración revisada no limita las operaciones de administración a un rol administrador.
* Las pruebas de `backend/kafka-admin` pasaron con `.\\gradlew.bat test`. Son pruebas unitarias/de controlador y no prueban envío o consumo contra un broker real.

## Brechas frente a las rúbricas

Las pautas EP5 y EP6 piden, entre otros aspectos:

* Configuración de cliente Kafka y conexión con los brokers.
* Productores y consumidores que publiquen y lean eventos de los tópicos, utilizando DTOs y una estructura coherente.
* Una infraestructura Kafka ejecutable con brokers y Kafka UI.
* Evidencia y explicación del flujo completo entre microservicios, desde la publicación hasta el consumo.
* Administración y demostración de tópicos y del clúster mediante las herramientas solicitadas.

Por lo tanto, el administrador Kafka es una base útil, pero todavía no demuestra por sí solo un flujo de mensajería de negocio.

## Alcance recomendado

1. **Mantener RabbitMQ funcionando.** No reemplazar ni duplicar indiscriminadamente los flujos existentes. Kafka se agrega para el flujo de eventos definido para la evaluación.
2. **Implementar el flujo Kafka mínimo de punta a punta.** Elegir eventos de pedido y/o alertas de inventario, definir sus DTOs y tópicos, publicar desde el microservicio productor y consumir desde el microservicio responsable de procesarlos.
3. **Conectar el consumo con notificaciones.** Para la demostración inicial, el consumidor puede procesar el evento y dejar una notificación verificable en logs o en el mecanismo disponible. Enviar correos, SMS o notificaciones web reales debe tratarse como alcance adicional salvo que la pauta o el docente lo exijan expresamente.
4. **Agregar la infraestructura pedida.** Crear y comprobar una configuración Docker Compose para Kafka con la arquitectura que defina el equipo y Kafka UI. Alinear direcciones de conexión, puertos y número de réplicas con esa configuración.
5. **Hacer explícitos los fallos de configuración.** Una falla de conexión o de creación de tópicos no debería confundirse con un arranque saludable del flujo. Definir qué debe bloquear el arranque y qué debe reflejarse en health checks/logs.
6. **Restringir la administración.** Proteger las operaciones que modifican tópicos para que solo pueda ejecutarlas un administrador.
7. **Probar y documentar una demostración reproducible.** Mostrar brokers saludables, tópicos, publicación, consumo y resultado de la notificación; documentar cómo iniciar cada componente.

## Orden de trabajo sugerido

### 1\. Confirmar decisiones y contratos

* Revisar con el equipo/docente qué eventos y canales de notificación deben demostrarse.
* Definir el nombre, versión y contenido de cada DTO de evento.
* Acordar qué microservicio publica y cuál consume cada tópico.
* Evitar que los eventos dependan de entidades JPA o detalles internos de una aplicación.

**Criterio de salida:** contratos y responsabilidades acordados antes de conectar productores y consumidores.

### 2\. Preparar Kafka local

* Añadir la configuración Compose de Kafka y Kafka UI solicitada por la evaluación.
* Configurar direcciones de acceso correctas tanto desde los contenedores como desde aplicaciones ejecutadas en el host.
* Comprobar que los brokers estén saludables y accesibles.
* Verificar que la cantidad de réplicas configurada para los tópicos sea compatible con los brokers disponibles.

**Criterio de salida:** se puede iniciar el clúster y ver sus brokers y tópicos en Kafka UI.

### 3\. Implementar productor y consumidor

* Configurar serialización/deserialización de eventos y grupos de consumidores.
* Publicar eventos en el punto adecuado del flujo de pedidos/inventario.
* Consumirlos y validarlos antes de procesarlos.
* Definir comportamiento observable para reintentos, errores y mensajes inválidos; evitar pérdida silenciosa.
* Considerar idempotencia para que la repetición de un evento no genere notificaciones duplicadas.

**Criterio de salida:** un evento publicado por el productor llega al consumidor y puede verificarse en logs o pruebas.

### 4\. Integrar el procesamiento de notificaciones

* Conectar el consumidor a un caso de uso de notificación, manteniendo separada la lógica de negocio del transporte Kafka.
* Si no se requiere un proveedor externo, implementar una salida verificable y explicar claramente que es una notificación simulada/de demostración.
* Si se exigen notificaciones reales, acordar primero proveedor, credenciales, costos y canales; no incluir secretos en el repositorio.

**Criterio de salida:** el evento consumido produce el resultado de notificación acordado y el comportamiento ante errores es visible.

### 5\. Seguridad, pruebas y demostración

* Limitar la administración de Kafka al rol correspondiente.
* Ejecutar pruebas unitarias y de integración con un broker real o de prueba.
* Verificar tanto el camino exitoso como un fallo recuperable y uno no recuperable.
* Probar RabbitMQ para confirmar que sus integraciones previas siguen funcionando.
* Preparar una demostración que explique arquitectura, tópicos, productores, consumidores y flujo de notificación.

**Criterio de salida:** el flujo puede repetirse desde un entorno limpio siguiendo instrucciones documentadas.

## Riesgos y notas

* **No confundir administración con mensajería:** `AdminClient` gestiona el clúster y los tópicos, pero no publica ni consume los eventos de negocio.
* **Réplicas:** un factor de réplica 2 necesita que el clúster tenga suficientes brokers disponibles. Si se usa un solo broker, la configuración actual de los tópicos no será viable sin ajustarla.
* **Direcciones de red:** `localhost` dentro de un contenedor se refiere a ese contenedor, no necesariamente al broker ni al host. Mantener separados los listeners internos y externos según quién se conecte.
* **Arranque resiliente:** decidir explícitamente si un servicio puede iniciar sin Kafka. Si se permite, debe quedar claro que el flujo de notificaciones no está disponible; si no se permite, el fallo debe detener el arranque.
* **Entrega gradual:** priorizar la infraestructura y un flujo funcional pequeño antes de implementar canales externos de notificación.

## Referencias del proyecto

* Rúbrica EP5: `contexto-caso/EP5\_DSY1107\_Estudiante\_encargo.pdf`
* Rúbrica EP6: `contexto-caso/EP6\_DSY1107\_Estudiante\_presentación.pdf`
* Caso de negocio: `contexto-caso/CasoPedidos360.pdf`
* Administrador Kafka: `backend/kafka-admin/`
* Configuración de tópicos: `backend/kafka-admin/src/main/resources/application.yml`
* Mensajería actual de pedidos: `backend/orders/src/main/java/com/hub/pedidos360/orders/messaging/`
* Infraestructura actual de RabbitMQ: `infra/rabbitmq/docker-compose.yml`

