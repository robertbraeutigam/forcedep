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
public class MergedPrivateMethodsDependenciesTests {
   private Dependencies delegate;
   private Dependencies.Object delegateObject;
   private Dependencies.Method delegateMethod;
   private MergedPrivateMethodsDependencies dependencies;

   public void testObjectsIsDelegated() {
      dependencies.object("a.B", new String[] {}).close();

      verify(delegate).object("a.B", new String[] {});
   }

   public void testMethodsAreDelegated() {
      Dependencies.Object object = dependencies.object("a.B", new String[] {});
      object.method("c", false);
      object.close();

      verify(delegateObject).method("c", false);
   }

   public void testMethodCallsAreDelegated() {
      Dependencies.Object object = dependencies.object("a.B", new String[] {});
      object.method("c", false).call("d.E", "f");
      object.close();

      verify(delegateMethod).call("d.E", "f");
   }

   public void testPrivateMethodsAreNotDelegated() {
      Dependencies.Object object = dependencies.object("a.B", new String[] {});
      object.method("c", true);
      object.close();

      verify(delegateObject, never()).method(eq("c"), anyBoolean());
   }

   public void testMethodsCalledFromPrivateMethodMergedIntoCallerLocalFirst() {
      Dependencies.Object object = dependencies.object("a.B", new String[] {});
      object.method("d", true).call("d.E", "f");
      object.method("c", false).call("a.B", "d");
      object.close();

      verify(delegateMethod).call("d.E", "f");
   }

   public void testMethodsCalledFromPrivateMethodMergedIntoCallerPublicFirst() {
      Dependencies.Object object = dependencies.object("a.B", new String[] {});
      object.method("c", false).call("a.B", "d");
      object.method("d", true).call("d.E", "f");
      object.close();

      verify(delegateMethod).call("d.E", "f");
   }

   public void testDoesNotIncludeLocalCallsToLocalMethods() {
      Dependencies.Object object = dependencies.object("a.B", new String[] {});
      object.method("d", true).call("d.E", "f");
      object.method("c", false).call("a.B", "d");
      object.close();

      verify(delegateMethod, never()).call("a.B", "d");
   }

   public void testIncludesLocalMethodCallsBetweenPublicMethods() {
      Dependencies.Object object = dependencies.object("a.B", new String[] {});
      object.method("d", false);
      object.method("c", false).call("a.B", "d");
      object.close();

      verify(delegateMethod).call("a.B", "d");
   }

   public void testMethodsCalledFromPrivateMethodChainIsFlattened() {
      Dependencies.Object object = dependencies.object("a.B", new String[] {});
      object.method("c", false).call("a.B", "d");
      object.method("d", true).call("a.B", "f");
      object.method("f", true).call("a.B", "g");
      object.method("g", true).call("a.B", "h");
      object.method("h", true).call("d.E", "i");
      object.close();

      verify(delegateMethod).call("d.E", "i");
      verify(delegateMethod).close();
      verifyNoMoreInteractions(delegateMethod);
   }

   @BeforeMethod
   protected void setUp() {
      delegate = mock(Dependencies.class);
      delegateObject = mock(Dependencies.Object.class);
      when(delegate.object(anyString(), anyVararg())).thenReturn(delegateObject);
      delegateMethod = mock(Dependencies.Method.class);
      when(delegateObject.method(anyString(), anyBoolean())).thenReturn(delegateMethod);
      dependencies = new MergedPrivateMethodsDependencies(delegate);
   }
}


