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

package com.vanillasource.forcedep.transform;

import com.vanillasource.forcedep.Dependencies;
import static org.mockito.Mockito.*;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

@Test
public class MergedLambdaDependenciesTests {
   private Dependencies delegate;
   private Dependencies.Object delegateObject;
   private Dependencies.Method delegateMethod;
   private MergedLambdaDependencies dependencies;

   public void testObjectsIsDelegated() {
      dependencies.object("a.B", false, false, new String[] {}).close();

      verify(delegate).object("a.B", false, false, new String[] {});
   }

   public void testMethodsAreDelegated() {
      Dependencies.Object object = dependencies.object("a.B", false, false, new String[] {});
      object.method("c", false).close();
      object.close();

      verify(delegateObject).method("c", false);
   }

   public void testMethodCallsAreDelegated() {
      Dependencies.Object object = dependencies.object("a.B", false, false, new String[] {});
      Dependencies.Method method = object.method("c", false);
      method.call("d.E", "f");
      method.close();
      object.close();

      verify(delegateMethod).call("d.E", "f");
   }

   public void testLambdaMethodsAreNotDelegated() {
      Dependencies.Object object = dependencies.object("a.B", false, false, new String[] {});
      object.method("access$100", false).call("d.E", "f");
      object.close();

      verify(delegateMethod, never()).call(any(), any());
   }

   public void testLambdaMethodsAreMergedToItsCaller() {
      Dependencies.Object object1 = dependencies.object("a.B", false, false, new String[] {});
      Dependencies.Method method1 = object1.method("access$100", false);
      method1.call("d.E", "f");
      method1.close();
      object1.close();

      Dependencies.Object object2 = dependencies.object("a.C", false, false, new String[] {});
      Dependencies.Method method2 = object2.method("d", false);
      method2.call("a.B", "access$100");
      method2.close();
      object2.close();

      verify(delegateMethod).call("d.E", "f");
      verify(delegateMethod).close();
      verifyNoMoreInteractions(delegateMethod);
   }

   @BeforeMethod
   protected void setUp() {
      delegate = mock(Dependencies.class);
      delegateObject = mock(Dependencies.Object.class);
      when(delegate.object(anyString(), anyBoolean(), anyBoolean(), anyVararg())).thenReturn(delegateObject);
      delegateMethod = mock(Dependencies.Method.class);
      when(delegateObject.method(anyString(), anyBoolean())).thenReturn(delegateMethod);
      dependencies = new MergedLambdaDependencies(delegate);
   }
}


