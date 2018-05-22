/**
 * Copyright (C) 2018 VanillaSource
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.vanillasource.forcedep.jvm;

import com.vanillasource.forcedep.Dependencies;
import static org.mockito.Mockito.*;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

@Test
public class AsmClassTests {
   private Dependencies dependencies;
   private Dependencies.Object object;
   private Dependencies.Method method;

   public void testObjectIsFound() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/jvm/B.class"));

      aClass.analyze(dependencies);

      verify(dependencies).object(eq("com.vanillasource.forcedep.jvm.B"), anyBoolean(), anyVararg());
   }

   public void testObjectIsClosed() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/jvm/B.class"));

      aClass.analyze(dependencies);

      verify(object).close();
   }

   public void testMethodIsFound() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/jvm/B.class"));

      aClass.analyze(dependencies);

      verify(object).method("b", false);
   }

   public void testMethodInvocationIsFound() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/jvm/B.class"));

      aClass.analyze(dependencies);

      verify(method).call("com.vanillasource.forcedep.jvm.A", "a");
   }

   public void testMethodIsClosed() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/jvm/B.class"));

      aClass.analyze(dependencies);

      verify(method, times(3)).close(); // 3 methods, including ctor
   }

   public void testConstructorInvocationIsFound() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/jvm/B.class"));

      aClass.analyze(dependencies);

      verify(method).call("com.vanillasource.forcedep.jvm.A", "<init>");
   }

   public void testInterfaceIsReadAsObject() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/jvm/C.class"));

      aClass.analyze(dependencies);

      verify(dependencies).object(eq("com.vanillasource.forcedep.jvm.C"), anyBoolean(), anyVararg());
   }

   public void testInterfaceMethodIsReadAsMethod() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/jvm/C.class"));

      aClass.analyze(dependencies);

      verify(object).method("c", false);
   }

   public void testPrivateMethodsAreRead() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/jvm/D.class"));

      aClass.analyze(dependencies);

      verify(object).method("d", false);
      verify(object).method("e", true);
   }

   public void testPrivateMethodsCallsAreDetected() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/jvm/D.class"));

      aClass.analyze(dependencies);

      verify(method).call("com.vanillasource.forcedep.jvm.A", "a");
   }

   public void testLambdaCallsAreDetected() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/jvm/E.class"));

      aClass.analyze(dependencies);

      verify(method).call("com.vanillasource.forcedep.jvm.A", "a");
   }

   public void testAnonymousInnerClassIsLocalObject() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/jvm/F$1.class"));

      aClass.analyze(dependencies);

      verify(dependencies).object(eq("com.vanillasource.forcedep.jvm.F$1"), eq(true), anyVararg());
   }

   public void testAnonymousInnerClassCallsAreDetected() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/jvm/F$1.class"));

      aClass.analyze(dependencies);

      verify(method).call(eq("com.vanillasource.forcedep.jvm.F"), any());
   }

   public void testFieldsAreDetected() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/jvm/G.class"));

      aClass.analyze(dependencies);

      verify(object).field("var");
   }

   public void testFieldAccessIsDetected() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/jvm/G.class"));

      aClass.analyze(dependencies);

      verify(method).reference("com.vanillasource.forcedep.jvm.G", "var");
   }

   @BeforeMethod
   protected void setUp() {
      dependencies = mock(Dependencies.class);
      method = mock(Dependencies.Method.class);
      object = mock(Dependencies.Object.class);
      when(dependencies.object(anyString(), anyBoolean(), anyVararg())).thenReturn(object);
      when(object.method(anyString(), anyBoolean())).thenReturn(method);
   }
}
