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

package com.vanillasource.forcedep.asm;

import com.vanillasource.forcedep.Dependencies;
import static org.mockito.Mockito.*;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

@Test
public class AsmClassTests {
   private Dependencies dependencies;

   public void testMethodInvocationIsFound() throws Exception {
      AsmClass aClass = new AsmClass(getClass().getClassLoader().getResourceAsStream("com/vanillasource/forcedep/asm/B.class"));

      aClass.analyze(dependencies);

      verify(dependencies).method("com.vanillasource.forcedep.asm.B", "b");
   }

   @BeforeMethod
   protected void setUp() {
      dependencies = mock(Dependencies.class);
   }
}
