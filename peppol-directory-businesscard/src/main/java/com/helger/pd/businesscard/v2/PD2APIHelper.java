/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.pd.businesscard.v2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Helper class for easier BC V2 creation.
 *
 * @author Philip Helger
 */
@Immutable
public final class PD2APIHelper
{
  private PD2APIHelper ()
  {}

  @Nonnull
  public static PD2IdentifierType createIdentifier (@Nullable final String sScheme, @Nullable final String sValue)
  {
    final PD2IdentifierType ret = new PD2IdentifierType ();
    ret.setScheme (sScheme);
    ret.setValue (sValue);
    return ret;
  }
}
