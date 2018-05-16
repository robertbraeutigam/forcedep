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

package com.vanillasource.forcedep.scan;

import com.vanillasource.forcedep.jvm.AsmClass;
import com.vanillasource.forcedep.Dependencies;
import static org.mockito.Mockito.*;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import java.io.File;

@Test
public class JarObjectsTests {
   private Dependencies dependencies;

   public void testAnalysisDetectsAllClasses() throws Exception {
      JarObjects jarObjects = new JarObjects(
            new File(getClass().getClassLoader().getResource("com/vanillasource/forcedep/scan/ab.jar").toURI()),
            AsmClass::new);

      jarObjects.analyze(dependencies);

      verify(dependencies).object(eq("com.vanillasource.forcedep.jvm.A"), any());
      verify(dependencies).object(eq("com.vanillasource.forcedep.jvm.B"), any());
   }

   @BeforeMethod
   protected void setUp() {
      dependencies = mock(Dependencies.class);
      Dependencies.Method method = mock(Dependencies.Method.class);
      Dependencies.Object object = mock(Dependencies.Object.class);
      when(dependencies.object(anyString(), any())).thenReturn(object);
      when(object.method(anyString())).thenReturn(method);
   }
}

