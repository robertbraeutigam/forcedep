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
public class MergedAnonymousClassesDependenciesTests {
   private Dependencies delegate;
   private Dependencies.Object delegateObject;
   private Dependencies.Method delegateMethod;
   private MergedAnonymousClassesDependencies dependencies;

   public void testObjectsIsDelegated() {
      dependencies.object("a.B", false, new String[] {}).close();
      dependencies.close();

      verify(delegate).object("a.B", false, new String[] {});
   }

   public void testMethodsAreDelegated() {
      Dependencies.Object object = dependencies.object("a.B", false, new String[] {});
      object.method("c", false).close();
      object.close();
      dependencies.close();

      verify(delegateObject).method("c", false);
   }

   public void testMethodCallsAreDelegated() {
      Dependencies.Object object = dependencies.object("a.B", false, new String[] {});
      Dependencies.Method method = object.method("c", false);
      method.call("d.E", "f");
      method.close();
      object.close();
      dependencies.close();

      verify(delegateMethod).call("d.E", "f");
   }

   public void testAnonymousClassesAreNotDelegated() {
      dependencies.object("a.B$1", true, new String[] {}).close();
      dependencies.close();

      verify(delegate).close();
      verifyNoMoreInteractions(delegate);
   }

   public void testMethodCallOnAnonymousMethodIsMergedToOwner() {
      Dependencies.Object object = dependencies.object("a.B", false, new String[] {});
      Dependencies.Method method = object.method("c", false);
      method.call("a.B$1", "<init>");
      method.close();
      object.close();

      Dependencies.Object object2 = dependencies.object("a.B$1", true, new String[] {});
      Dependencies.Method method2 = object.method("f", false);
      method2.call("d.E", "g");
      method2.close();
      object2.close();

      dependencies.close();

      verify(delegateMethod).call("d.E", "g");
   }

   public void testInstantiatonOfAnonymousClassIsNotVisible() {
      Dependencies.Object object = dependencies.object("a.B", false, new String[] {});
      Dependencies.Method method = object.method("c", false);
      method.call("a.B$1", "<init>");
      method.close();
      object.close();

      Dependencies.Object object2 = dependencies.object("a.B$1", true, new String[] {});
      Dependencies.Method method2 = object.method("f", false);
      method2.call("d.E", "g");
      method2.close();
      object2.close();

      dependencies.close();

      verify(delegateMethod, never()).call(any(), eq("<init>"));
   }

   public void testTransitiveMethodCallOnAnonymousMethodIsMergedToOwner() {
      Dependencies.Object object = dependencies.object("a.B", false, new String[] {});
      Dependencies.Method method = object.method("c", false);
      method.call("a.B$1", "<init>");
      method.close();
      object.close();

      Dependencies.Object object2 = dependencies.object("a.B$1", true, new String[] {});
      Dependencies.Method method2 = object.method("f", false);
      method2.call("a.B$1$1", "<init>");
      method2.close();
      object2.close();

      Dependencies.Object object3 = dependencies.object("a.B$1$1", true, new String[] {});
      Dependencies.Method method3 = object.method("f", false);
      method3.call("d.E", "g");
      method3.close();
      object3.close();

      dependencies.close();

      verify(delegateMethod).call("d.E", "g");
      verify(delegateMethod, never()).call(any(), eq("<init>"));
   }

   @BeforeMethod
   protected void setUp() {
      delegate = mock(Dependencies.class);
      delegateObject = mock(Dependencies.Object.class);
      when(delegate.object(anyString(), anyBoolean(), anyVararg())).thenReturn(delegateObject);
      delegateMethod = mock(Dependencies.Method.class);
      when(delegateObject.method(anyString(), anyBoolean())).thenReturn(delegateMethod);
      dependencies = new MergedAnonymousClassesDependencies(delegate);
   }
}


