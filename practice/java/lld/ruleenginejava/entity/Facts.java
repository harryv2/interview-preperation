package lld.ruleenginejava.entity;

import java.util.Map;

public class Facts {
    private final Map<String, Object> values;

    public Facts(Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    public Object get(String key) {
        return values.get(key);
    }

    public Double number(String key) {
        Object value = values.get(key);
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return null;
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
