package org.deuce.utest.point.jvstm.units.gc;

import java.lang.reflect.Field;

import jvstm.VBox;

import org.deuce.transform.Exclude;
import org.deuce.utest.point.Point;
import org.deuce.utest.point.PointFields;
import org.deuce.utest.point.jvstm.StaticShortPoint;
import org.deuce.utest.point.jvstm.StaticShortPointFactory;
import org.deuce.utest.point.jvstm.runners.RunTestGcBodiesHistory;

/**
 * !!!! This test must run with the GC disabled:
 *   -Djvstm.aom.reversion.disabled=true
 */
@Exclude
public class StaticShortPointGcTest extends RunTestGcBodiesHistory<Short>{

	static PointFields<Short> fields;

	static{
		try {
			fields = new PointFields<Short>(
					(Class<Point<Short>>) Class.forName(
							StaticShortPoint.class.getName() + "__STATICFIELDS__"));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	public StaticShortPointGcTest() {
		super(new StaticShortPointFactory(), fields);
	}

	/**
	 * In the JVSTM following the AOM approach the transactional objects with
	 * static fields include a unique STATIC_PART$ static field pointing to
	 * a singleton object, which contains all the original static fields, but
	 * in instance fields.  
	 */
	@Override
	protected VBox<Point<Short>> getVBox(Point<Short> p) {
		try {
			Field fieldStaticPart = p.getClass().getDeclaredField("STATIC_PART$");
			VBox<Point<Short>> staticPart = (VBox<Point<Short>>) fieldStaticPart.get(null);
			return staticPart;
		} 
		catch (NoSuchFieldException e) {throw new RuntimeException(e);}
		catch (SecurityException e) {throw new RuntimeException(e);}
		catch (IllegalAccessException e) {throw new RuntimeException(e);}
	}
}
