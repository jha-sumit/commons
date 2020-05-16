package org.fish.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * Common Builder for any Class with default constructor.
 * <pre>example {@code
 *  FluentBuilder.of(Type.class)
 *      .map(Type::setter, () -> d1)
 *      .map(Type::anotherSetter, () -> d2)
 *      .amp(Type::anotherSetter, () -> d3, (type, d3) -> validate(d3) ? true: false)
 *      .build();
 * }
 * </pre>
 *
 * @author Sumit Jha
 * @version 1.0
 */
public class FluentBuilder<T> {
    final Logger log = LoggerFactory.getLogger(FluentBuilder.class);
    private final Class<T> cls;

    public FluentBuilder(Class<T> cls) {
        this.cls = cls;
    }

    public static <T> FluentBuilder<T> of(Class<T> cls) {
        return new FluentBuilder<>(cls);
    }

    public <U> FluentBuilder<T> map(BiConsumer<T, U> consumer, Supplier<U> supplier) {
        return doMap(consumer, supplier);
    }

    public <U> FluentBuilder<T> map(BiConsumer<T, U> consumer, Supplier<U> supplier, BiPredicate<T, U> matcher) {
        return doMap(consumer, supplier, matcher);
    }

    private <U> FluentBuilder<T> doMap(BiConsumer<T, U> consumer, Supplier<U> supplier) {
        return new Operation(this) {
            @Override
            T doEvaluate(T object) {
                consumer.accept(object, supplier.get());
                return object;
            }
        };
    }

    private <U> FluentBuilder<T> doMap(BiConsumer<T, U> consumer, Supplier<U> supplier, BiPredicate<T, U> matcher) {
        return new Operation(this) {
            @Override
            T doEvaluate(T object) {
                if (matcher.test(object, supplier.get())) {
                    consumer.accept(object, supplier.get());
                }
                return object;
            }
        };
    }

    public T build() {
        try {
            log.debug("Starting build operation!!!");
            return continueBuild(cls.newInstance());
        } catch (InstantiationException | IllegalAccessException ex) {
            log.info("Failed to initiate the object of {}. Make sure class has default public conctructor.", cls);
        }
        log.debug("Object can not be created, returning NULL");
        return null;
    }

    T continueBuild(T object) {
        return object;
    }

    abstract class Operation extends FluentBuilder<T> {
        protected final FluentBuilder<T> head;

        public Operation(FluentBuilder<T> head) {
            super(cls);
            this.head = head;
        }

        abstract T doEvaluate(T object);

        @Override
        T continueBuild(T object) {
            return head.continueBuild(doEvaluate(object));
        }
    }
}
