package org.vrspace.server.obj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class VRObjectTest {

  // due to the fact that equals and hash code are generated, they can be messed
  // up really easy
  @Test
  public void testEquals() {
    VRObject o1 = new VRObject(1L);
    VRObject o2 = new VRObject(1L);
    VRObject o3 = new VRObject(3L);
    VRObject t1 = new VRObject(101L, o1);
    VRObject t2 = new VRObject(101L, o2);
    VRObject t3 = new VRObject(103L, o1);

    assertEquals(o1, o2);
    assertEquals(o2, o1);
    assertFalse(o1.equals(o3));
    assertFalse(o2.equals(o3));
    assertFalse(o3.equals(o1));
    assertFalse(o3.equals(o2));

    assertEquals(t1, t2);
    assertEquals(t2, t1);
    assertFalse(t1.equals(t3));
    assertFalse(t3.equals(t1));
    assertFalse(o1.equals(t1));
    assertFalse(t1.equals(o1));

    assertEquals(o1, t1.getChildren().get(0));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testChildren() throws Exception {
    new VRObject(1L, new VRObject(1L));
  }
}
