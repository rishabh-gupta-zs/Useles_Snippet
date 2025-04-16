import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class InstanceFactory {
    private static final Map<Key, Object>      instances = new ConcurrentHashMap<>();
    private static final Map<Key, Supplier<?>> suppliers = new ConcurrentHashMap<>();

    public static <T> void register(Class<T> clazz, Supplier<?> supplier) {
        register(clazz, null, supplier);
    }

    public static <T> void register(Class<T> clazz, String qualifier, Supplier<?> supplier) {
        suppliers.put(Key.of(clazz, qualifier), supplier);
    }

    public static <T> T get(Class<T> clazz) {
        return get(clazz, null);
    }

    public static <T> T get(Class<T> clazz, String qualifier) {
        return clazz.cast(
            instances.computeIfAbsent(Key.of(clazz, qualifier), k -> {
                Supplier<?> supplier = suppliers.get(k);
                if (supplier == null) {
                    throw new IllegalArgumentException("No supplier register for " + k);
                }
                return supplier.get();
            })
        );
    }

    public static void main(String[] args) {
        InstanceFactory.register(Logger.class, Logger::new);
        InstanceFactory.register(User.class, () -> new User("Default"));
        InstanceFactory.register(User.class, "Another", () -> new User("Another"));

        System.out.println(InstanceFactory.get(Logger.class));
        System.out.println(InstanceFactory.get(User.class));
        System.out.println(InstanceFactory.get(User.class, "Another"));
    }

    @EqualsAndHashCode
    static class Key {
        private final Class<?> clazz;
        private final String   qulifier;

        public static Key of(Class<?> clazz, String qulifier) {
            return new Key(clazz, qulifier);
        }
        private Key(Class<?> clazz, String qulifier) {
            this.clazz = clazz;
            this.qulifier = qulifier;
        }
        @Override
        public String toString() {
            return "clazz: " + clazz +
                " and qulifier: '" + qulifier;
        }
    }

    @ToString
    static class Logger {
        public Logger() {
            System.out.println("logger created");
        }
    }
    @ToString
    static class User {
        String name;
        public User(String name) {
            this.name = name;
            System.out.println("User created with " + name);
        }
    }
}
