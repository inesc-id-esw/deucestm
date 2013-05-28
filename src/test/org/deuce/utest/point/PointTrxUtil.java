package org.deuce.utest.point;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jvstm.SuspendedTransaction;
import jvstm.Transaction;
import jvstm.UtilUnsafe;
import jvstm.VBox;
import jvstm.VBoxBody;

import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transform.Exclude;

@Exclude
public class PointTrxUtil {
	static final Context ctx;
	static final Field currTrx;
	static final Method getX, getY, setX, setY;
	static{
		ctx = ContextDelegator.getInstance();
		if(!(ctx instanceof org.deuce.transaction.jvstm.Context))
			throw new IllegalStateException("Illegal configuration! This unit test is just conform with the JVSTM-AOM STM!");
		try {
			currTrx = ctx.getClass().getDeclaredField("currentTrx");
			currTrx.setAccessible(true);
			getX = Point.class.getDeclaredMethod("getX", Context.class);
			getX.setAccessible(true);
			getY = Point.class.getDeclaredMethod("getY", Context.class);
			getY.setAccessible(true);
			setX = Point.class.getDeclaredMethod("setX", Number.class, Context.class);
			setX.setAccessible(true);
			setY = Point.class.getDeclaredMethod("setY", Number.class, Context.class);
			setY.setAccessible(true);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);	}
	}
	public static <T extends Number> Number getX(final Point<T> p){
		try {
			return (Number) getX.invoke(p, ctx);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} 
	}
	public static <T extends Number> Number getY(final Point<T> p){
		try {
			return (Number) getY.invoke(p, ctx);
		}  catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	public static <T extends Number> void setX(final Point<T> p, Number n){
		try {
			setX.invoke(p, n, ctx);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} 
	}
	public static <T extends Number> void setY(final Point<T> p, Number n){
		try {
			setY.invoke(p, n, ctx);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} 
	}
	public static Transaction begin(boolean readonly){
		Transaction trx = Transaction.begin(readonly);
		try {
			currTrx.set(ctx, trx);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		return trx;
	}
	public static void commit(){
		Transaction.commit();
		try {
			currTrx.set(ctx, null);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static SuspendedTransaction suspendTx(Transaction trx)throws Exception{
		SuspendedTransaction t = trx.suspendTx();
		currTrx.set(ctx, null);
		return t;
	}
	public static void resume(Transaction trx, SuspendedTransaction t){
		Transaction.resume(t);
		try {
			currTrx.set(ctx, trx);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	public static void printStaticUnifiedField(Class<?> klass, String fieldName){
		try {
			Field fieldStaticPart = klass.getDeclaredField("STATIC_PART$");
			Field fieldX = Class.forName(klass.getName() + "__STATICFIELDS__").getDeclaredField(fieldName);
			Object staticPart = fieldStaticPart.get(null);
			System.out.println(fieldX.get(staticPart));

		} catch (NoSuchFieldException e) {throw new RuntimeException(e);
		} catch (SecurityException e) {throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {throw new RuntimeException(e);
		} catch (IllegalAccessException e) {throw new RuntimeException(e);
		}
	}

	static final long BODY_OFFSET = UtilUnsafe.objectFieldOffset(VBox.class, "body");

	public static void staticPartToCompactLayout(Class<?> klass){
		try {
			Field fieldStaticPart = klass.getDeclaredField("STATIC_PART$");
			VBox staticPart = (VBox<?>) fieldStaticPart.get(null);
			/*
	    VBoxBody header = staticPart.body;
	    if(header != null){
		staticPart.toCompactLayout(header.value);
		UtilUnsafe.UNSAFE.compareAndSwapObject(staticPart, BODY_OFFSET, header, null);
	    }
			 */
			staticPart.body = new VBoxBody(staticPart, 0, null);
		} 
		catch (NoSuchFieldException e) {throw new RuntimeException(e);}
		catch (SecurityException e) {throw new RuntimeException(e);}
		catch (IllegalAccessException e) {throw new RuntimeException(e);}
	}
	public static void staticPartPrintHistory(Class<?> klass){
		try {
			Field fieldStaticPart = klass.getDeclaredField("STATIC_PART$");
			VBox staticPart = (VBox) fieldStaticPart.get(null);
			String history = "history = ";
			for(VBoxBody header = staticPart.body; header != null; header = header.next){
				history += header.version + " -> ";
			}
			System.out.println(history);
		} 
		catch (NoSuchFieldException e) {throw new RuntimeException(e);}
		catch (SecurityException e) {throw new RuntimeException(e);}
		catch (IllegalAccessException e) {throw new RuntimeException(e);}
	}
	public static void printHistory(Object ref){
		String history = "history = ";
		for(VBoxBody header = ((VBox) ref).body; header != null; header = header.next){
			history += header.version + "(" + header.value + ") -> ";
		}
		System.out.println(history);
	}
	public static void main(String[] args){
		System.out.println(currTrx);
	}
}
