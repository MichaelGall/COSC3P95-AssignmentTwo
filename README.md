# COSC 3P95 Assignment Two

Michael Gall - Brock University - #6106397

Sources:
- Original app (linked in assignment): https://github.com/sohamds1/spring-boot-todo-app
- OpenTelemetry's Java Example: https://opentelemetry.io/docs/demo/services/ad/
- OpenTelemetry's Manual Java Implementation: https://opentelemetry.io/docs/instrumentation/java/manual/

Most work was completed inside of the `TodoController.java` class in regards to the spans/metrics 
- Parent spans with children are implemented for most routes inside of the controller
- Metric tracking is there, with "redrawing" the todos being tracked.
- I attempted to implement a CPU usage tracker, but had a lot of issues.

Included are photos (PNGs) of the visualizations done as part of the assignment inside of Jaeger.

There is also a PDF with the overall implementation notes.
