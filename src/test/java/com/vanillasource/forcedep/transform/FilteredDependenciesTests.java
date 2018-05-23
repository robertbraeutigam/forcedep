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
import static java.util.Arrays.asList;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

@Test
public class FilteredDependenciesTests {
   private Dependencies delegate;
   private Dependencies.Object delegateObject;
   private Dependencies.Method delegateMethod;
   private FilteredDependencies dependencies;

   public void testEmptyFilterDoesNotDelegateAnything() {
      dependencies = new FilteredDependencies(asList(), asList(), delegate);

      dependencies.object("a.B", false, new String[] {});

      verify(delegate, never()).object("a.B", false, new String[] {});
   }

   public void testObjectsIsDelegatedIfOnWhitelist() {
      dependencies = new FilteredDependencies(asList("a.B"), asList(), delegate);

      dependencies.object("a.B", false, new String[] {});

      verify(delegate).object("a.B", false, new String[] {});
   }

   public void testObjectsIsDelegatedIfOnWhitelistAsRegexp() {
      dependencies = new FilteredDependencies(asList(".*"), asList(), delegate);

      dependencies.object("a.B", false, new String[] {});

      verify(delegate).object("a.B", false, new String[] {});
   }

   public void testObjectsIsNotDelegatedIfOnWhitelistButAlsoOnBlacklist() {
      dependencies = new FilteredDependencies(asList(".*"), asList("a.B"), delegate);

      dependencies.object("a.B", false, new String[] {});

      verify(delegate, never()).object("a.B", false, new String[] {});
   }

   @BeforeMethod
   protected void setUp() {
      delegate = mock(Dependencies.class);
      delegateObject = mock(Dependencies.Object.class);
      when(delegate.object(anyString(), anyBoolean(), anyVararg())).thenReturn(delegateObject);
      delegateMethod = mock(Dependencies.Method.class);
      when(delegateObject.method(anyString(), anyBoolean())).thenReturn(delegateMethod);
   }
}

