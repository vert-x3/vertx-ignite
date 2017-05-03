package io.vertx.spi.cluster.ignite.cfg;

import java.lang.reflect.Field;

import org.apache.ignite.events.EventType;

import com.fasterxml.jackson.databind.util.StdConverter;

public class EventTypeConverter extends StdConverter<Object[],int[]> {

  @Override
  public int[] convert(Object[] value) {
    if(value == null)
      return null;
    else {
      int[] result = new int[value.length];
      for (int i = 0; i < value.length; i++) {
        Object object = value[i];
        if(object instanceof Number) {
          result[i] = ((Number) object).intValue();
        } else if(object instanceof String) {
          try {
            Field declaredField = EventType.class.getDeclaredField(((String) object).toUpperCase());
            result[i] = (int) declaredField.get(EventType.class);
          } catch (Exception e) {
            throw new IllegalArgumentException("Illegal value "+object+" for IncludeEventTypes, event must be int or constant from EventType class");
          }
        } else {
          throw new IllegalArgumentException("Illegal value "+object+" for IncludeEventTypes, event must be int or constant from EventType class");
        }
      }
      return result;
    }    
  }

}
