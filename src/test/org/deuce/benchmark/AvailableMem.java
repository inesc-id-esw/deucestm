package org.deuce.benchmark;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AvailableMem {
    public static void main(String[] args) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        Method m = os.getClass().getDeclaredMethod("getFreePhysicalMemorySize");
        m.setAccessible(true);
        long mem = ((Long) m.invoke(os)).longValue()/1024/1024;
        mem -= 256;
        System.out.println(mem > 0? mem: 256);
     }
}
