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

package com.helger.pd.businesscard.generic;

import java.io.Serializable;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;

/**
 * Generic identifier.
 * 
 * @author Philip Helger
 */
@Immutable
public class PDIdentifier implements Serializable
{
  private final String m_sScheme;
  private final String m_sValue;

  public PDIdentifier (@Nullable final String sScheme, @Nullable final String sValue)
  {
    m_sScheme = sScheme;
    m_sValue = sValue;
  }

  /**
   * @return The identifier scheme. May be <code>null</code>.
   */
  @Nullable
  public String getScheme ()
  {
    return m_sScheme;
  }

  /**
   * @return The identifier value. May be <code>null</code>.
   */
  @Nullable
  public String getValue ()
  {
    return m_sValue;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;

    final PDIdentifier rhs = (PDIdentifier) o;
    return EqualsHelper.equals (m_sScheme, rhs.m_sScheme) && EqualsHelper.equals (m_sValue, rhs.m_sValue);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sScheme).append (m_sValue).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Scheme", m_sScheme).append ("Value", m_sValue).getToString ();
  }
}