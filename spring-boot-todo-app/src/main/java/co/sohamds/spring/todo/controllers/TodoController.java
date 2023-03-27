package co.sohamds.spring.todo.controllers;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;

import java.lang.management.ManagementFactory;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import co.sohamds.spring.todo.domain.Todo;
import co.sohamds.spring.todo.repository.TodoRepository;

import com.sun.management.OperatingSystemMXBean;

///
// Michael Gall - COSC 3P95 - Brock University
// #6106397
// - Most of my work was done in this class
// - Chose to manually implement OpenTelemetry
// - Using Jaegar as the logging service for trace data
// - Refer to the report PDF for more information on the sources and implementation notes
///

@Controller
public class TodoController {
	Tracer tracer = GlobalOpenTelemetry.getTracer("jaeger", "1.0.0");
	Meter meter = GlobalOpenTelemetry.meterBuilder("jaeger").setInstrumentationVersion("1.0.0").build();

	final LongCounter todoDrawsCounter = meter.counterBuilder("COSC3P95.todos.redraws")
			.setDescription("Number of total todo items redrawn.").setUnit("1").build();
	AttributeKey<String> todoDrawCountKey = AttributeKey.stringKey("COSC3P95.todos.count");

	// Attempted CPU Usage Tracker
	OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

//	ObservableDoubleGauge cpuGauge = meter
//			  .gaugeBuilder("cpu_usage")
//			  .setDescription("CPU Usage")
//			  .setUnit("ms")
//			  .buildWithCallback(measurement -> {
//			    measurement.record(osBean.getProcessCpuLoad(), Attributes.of(AttributeKey<"Key">, "SomeWork"));
//			  });

	@Autowired
	TodoRepository todoRepository;

	@GetMapping
	public String index() {
		return "index.html";
	}

	@GetMapping("/todos")
	public String todos(Model model) {
		Span listSpan = tracer.spanBuilder("list-todos").startSpan();
		
		todoDrawsCounter.add(1, Attributes.of(todoDrawCountKey,  Integer.toString((((Collection<?>) todoRepository.findAll()).size()))));
		model.addAttribute("todos", todoRepository.findAll());

		listSpan.end();

		return "todos";
	}

	@PostMapping("/todoNew")
	public String add(@RequestParam String todoItem, @RequestParam String status, Model model) {
		// Create the overall delete event's span
		Span addSpan = tracer.spanBuilder("add-todo").startSpan();

		// Create and run a span for creating the new todo object
		Span createTodoSpan = tracer.spanBuilder("create-new-todo-item").setParent(Context.current().with(addSpan))
				.startSpan();
		// Original Code
		Todo todo = new Todo(todoItem, status);
		todo.setTodoItem(todoItem);
		todo.setCompleted(status);
		//
		createTodoSpan.setAttribute("COSC3P95.addedTodoItem", todoItem);
		createTodoSpan.setAttribute("COSC3P95.addedTodoItemStatus", status);
		createTodoSpan.end();

		// Create and run a span for saving the new todo item to the collection
		Span saveSpan = tracer.spanBuilder("save-new-todo-item").setParent(Context.current().with(addSpan)).startSpan();
		// Original Code
		todoRepository.save(todo);
		//
		saveSpan.end();

		// Create and run a span for relisting all todos after adding a new one
		Span relistTodoSpan = tracer.spanBuilder("relist-todos-after-adding").setParent(Context.current().with(addSpan))
				.startSpan();
		// Original Code
		model.addAttribute("todos", todoRepository.findAll());
		//
		relistTodoSpan.setAttribute("COSC3P95.relistedTodosCount", (((Collection<?>) todoRepository.findAll()).size()));
		relistTodoSpan.end();
		// End the parent span
		addSpan.end();

		return "redirect:/todos";
	}

	@PostMapping("/todoDelete/{id}")
	public String delete(@PathVariable long id, Model model) {
		// Create the overall delete event's span
		Span deleteSpan = tracer.spanBuilder("delete-todo").startSpan();

		// Create and run a span for actually deleting the Todo by it's ID
		Span deleteByIdSpan = tracer.spanBuilder("delete-todo-by-id").setParent(Context.current().with(deleteSpan))
				.startSpan();
		// Original Code
		todoRepository.deleteById(id);
		//
		deleteByIdSpan.setAttribute("COSC3P95.idToDelete", Long.toString(id));
		deleteByIdSpan.end();

		// Create and run a span for relisting the todos after deletion
		Span relistTodoSpan = tracer.spanBuilder("relist-todos-after-delete")
				.setParent(Context.current().with(deleteSpan)).startSpan();
		// Original Code
		model.addAttribute("todos", todoRepository.findAll());
		//
		relistTodoSpan.setAttribute("COSC3P95.relistedTodosCount", (((Collection<?>) todoRepository.findAll()).size()));
		relistTodoSpan.end();
		// End the parent span
		deleteSpan.end();

		return "redirect:/todos";
	}

	@PostMapping("/todoUpdate/{id}")
	public String update(@PathVariable long id, Model model) {
		// Create the overall delete event's span
		Span updateSpan = tracer.spanBuilder("update-todo").startSpan();

		// Create and run a span for finding the todo by it's ID
		Span findByIdSpan = tracer.spanBuilder("find-todo-by-id").setParent(Context.current().with(updateSpan))
				.startSpan();
		// Original Code
		Todo todo = todoRepository.findById(id).get();
		//
		findByIdSpan.setAttribute("COSC3P95.idOfInterest", Long.toString(id));
		findByIdSpan.end();

		Span determiningStatusSpan = tracer.spanBuilder("save-new-todo-status")
				.setParent(Context.current().with(updateSpan)).startSpan();
		// Original Code
		if ("Yes".equals(todo.getCompleted())) {
			todo.setCompleted("No");
		} else {
			todo.setCompleted("Yes");
		}
		todoRepository.save(todo);
		//
		determiningStatusSpan.setAttribute("COSC3P95.newTodoStatus", todo.getCompleted());
		determiningStatusSpan.end();

		// Create and run a span for relisting the todos after updating
		Span relistTodoSpan = tracer.spanBuilder("relist-todos-after-update")
				.setParent(Context.current().with(updateSpan)).startSpan();
		// Original Code
		model.addAttribute("todos", todoRepository.findAll());
		//
		relistTodoSpan.setAttribute("COSC3P95.relistedTodosCount", (((Collection<?>) todoRepository.findAll()).size()));
		relistTodoSpan.end();
		// End the parent span
		updateSpan.end();

		return "redirect:/todos";
	}
}