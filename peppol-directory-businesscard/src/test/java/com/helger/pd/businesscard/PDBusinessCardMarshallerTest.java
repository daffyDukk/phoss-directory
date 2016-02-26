/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.pd.businesscard;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.helger.commons.io.resource.FileSystemResource;

/**
 * Test class for class {@link PDBusinessCardMarshaller}.
 *
 * @author Philip Helger
 */
public final class PDBusinessCardMarshallerTest
{
  @Test
  public void testBasic ()
  {
    final PDBusinessCardMarshaller aMarshaller = new PDBusinessCardMarshaller ();
    assertNotNull (aMarshaller.read (new FileSystemResource ("src/test/resources/example/business-card-test1.xml")));
    assertNotNull (aMarshaller.read (new FileSystemResource ("src/test/resources/example/business-card-example-spec.xml")));
  }
}
