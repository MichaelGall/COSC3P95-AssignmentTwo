package co.sohamds.spring.todo;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import co.sohamds.spring.todo.domain.Todo;
import co.sohamds.spring.todo.repository.TodoRepository;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

@SpringBootApplication
public class SpringBootTodoAppApplication implements CommandLineRunner {
	@Autowired
	public TodoRepository todoRepository;

	// Michael Gall - COSC 3P95
	// Implementing Manual Instrumentation for OpenTelemetry
	Resource resource = Resource.getDefault()
			.merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "cosc3p95-spring-boot-app")));

	SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
			.addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().build()).build())
			.setResource(resource).build();

	SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder()
			.registerMetricReader(PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder().build()).build())
			.setResource(resource).build();

	OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider)
			.setMeterProvider(sdkMeterProvider)
			.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
			.buildAndRegisterGlobal();

	public static void main(String[] args) {

		SpringApplication.run(SpringBootTodoAppApplication.class, args);

	}

	@Override
	public void run(String... args) throws Exception {
		// Michael Gall - COSC 3P95
		// Create and run the span for the boot sequence
		Tracer tracer = GlobalOpenTelemetry.getTracer("jaeger", "1.0.0");
		Span span = tracer.spanBuilder("begin-todo-application").startSpan();
		Scope scope = span.makeCurrent();
		
		Todo test = Todo.builder().id(10).completed("its completed").todoItem("python ML").build();
		System.out.println(test.toString());
		List<Todo> todos = Arrays.asList(new Todo("Learn Spring", "Yes"), new Todo("Learn Driving", "No"),
				new Todo("Go for a Walk", "No"), new Todo("Cook Dinner", "Yes"));
		todos.forEach(todoRepository::save);
		
		// End it once the app is finished creating
		span.end();

	}
}
