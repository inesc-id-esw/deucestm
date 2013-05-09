package org.deuce.utest.point.jvstm.units.gc;

import java.lang.reflect.Field;

import jvstm.VBox;

import org.deuce.transform.Exclude;
import org.deuce.utest.point.Point;
import org.deuce.utest.point.PointFields;
import org.deuce.utest.point.jvstm.StaticIntegerPoint;
import org.deuce.utest.point.jvstm.StaticIntegerPointFactory;
import org.deuce.utest.point.jvstm.runners.RunTestGcBodiesHistory;

/**
 * !!!! This test must run with the GC disabled:
 *   -Djvstm.aom.reversion.disabled=true
 */
@Exclude
public class StaticIntegerPointGcTest extends RunTestGcBodiesHistory<Integer>{

	static PointFields<Integer> fields;

	static{
		try {
			fields = new PointFields<Integer>(
					(Class<Point<Integer>>) Class.forName(
							StaticIntegerPoint.class.getName() + "__STATICFIELDS__"));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	public StaticIntegerPointGcTest() {
		super(new StaticIntegerPointFactory(), fields);
	}

	/**
	 * In the JVSTM following the AOM approach the transactional objects with
	 * static fields include a unique STATIC_PART$ static field pointing to
	 * a singleton object, which contains all the original static fields, but
	 * in instance fields.  
	 */
	@Override
	protected VBox<Point<Integer>> getVBox(Point<Integer> p) {
		try {
			Field fieldStaticPart = p.getClass().getDeclaredField("STATIC_PART$");
			VBox<Point<Integer>> staticPart = (VBox<Point<Integer>>) fieldStaticPart.get(null);
			return staticPart;
		} 
		catch (NoSuchFieldException e) {throw new RuntimeException(e);}
		catch (SecurityException e) {throw new RuntimeException(e);}
		catch (IllegalAccessException e) {throw new RuntimeException(e);}
	}
}
