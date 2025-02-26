package main;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class TypeSafeMap {
    private final Map<String, Object> valueMap = new HashMap<>();
    private final Map<String, Type> typeMap = new HashMap<>();

    public <T> void put(String key, T value, Class<?>... typeArguments) {
        valueMap.put(key, value);
        if (typeArguments.length > 0) {
            typeMap.put(key, new ParameterizedTypeImpl(value.getClass(), typeArguments));
        } else {
            typeMap.put(key, value.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> rawType, Class<?>... typeArguments) {
        Object value = valueMap.get(key);
        Type storedType = typeMap.get(key);

        if (isTypeCompatible(storedType, rawType, typeArguments)) {
            return (T) value;
        }
        throw new ClassCastException(storedType + "' is not of type " + createTypeName(rawType, typeArguments));
    }

    private boolean isTypeCompatible(Type storedType, Class<?> rawType, Class<?>... typeArguments) {
        if (storedType instanceof Class<?>) {
            Class<?> storedClass = (Class<?>) storedType;
            if (rawType.isAssignableFrom(storedClass)) {
                return typeArguments.length == 0;
            }
        }
        if (storedType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) storedType;
            if (rawType.isAssignableFrom((Class<?>) paramType.getRawType())) {
                Type[] storedArgs = paramType.getActualTypeArguments();
                return areTypeArgumentsCompatible(storedArgs, typeArguments);
            }
        }
        return false;
    }

    private boolean areTypeArgumentsCompatible(Type[] stored, Class<?>[] requested) {
        if (stored.length != requested.length) {
            return false;
        }
        for (int i = 0; i < stored.length; i++) {
            if (!stored[i].equals(requested[i])) {
                return false;
            }
        }
        return true;
    }

    private String createTypeName(Class<?> rawType, Class<?>... typeArguments) {
        if (typeArguments.length == 0) {
            return rawType.getSimpleName();
        }
        StringBuilder sb = new StringBuilder(rawType.getSimpleName()).append("<");
        for (int i = 0; i < typeArguments.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(typeArguments[i].getSimpleName());
        }
        return sb.append(">").toString();
    }

    private static class ParameterizedTypeImpl implements ParameterizedType {
        private final Class<?> rawType;
        private final Type[] typeArguments;

        public ParameterizedTypeImpl(Class<?> rawType, Type... typeArguments) {
            this.rawType = rawType;
            this.typeArguments = typeArguments;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return typeArguments;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(rawType.getSimpleName());
            if (typeArguments != null) {
                sb.append(typeArguments.length > 0 ? "<" + typeArguments[0] + ">" : ""  );
            }

            return sb.toString();
        }
    }

    public static void main(String[] args) {
        TypeSafeMap map = new TypeSafeMap();

        List<Customer> customers = new ArrayList<>();
        customers.add(new Customer("Alice", 30));
        customers.add(new Customer("Bob", 25));
        customers.add(new Customer("Charlie", 35));

        map.put("customerList", customers, Customer.class);

        // List<Customer> 데이터 가져오기
        List<Customer> customerList = map.get("customerList", List.class, Customer.class);

        // 고객 정보 출력
        for (Customer customer : customerList) {
            System.out.println(customer);
        }
    }
}