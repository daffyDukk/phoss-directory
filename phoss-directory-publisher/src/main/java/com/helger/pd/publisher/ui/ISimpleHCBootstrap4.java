/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.html.hc.IHCNode;
import com.helger.photon.bootstrap4.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap4.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap4.alert.BootstrapQuestionBox;
import com.helger.photon.bootstrap4.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap4.alert.BootstrapWarnBox;

/**
 * Traits interface to add simpler UI codes.
 *
 * @author Philip Helger
 */
public interface ISimpleHCBootstrap4 extends ISimpleHC
{
  @Nonnull
  default BootstrapErrorBox error ()
  {
    return new BootstrapErrorBox ();
  }

  @Nonnull
  default BootstrapErrorBox error (@Nullable final IHCNode aNode)
  {
    return new BootstrapErrorBox ().addChild (aNode);
  }

  @Nonnull
  default BootstrapErrorBox error (@Nullable final String s)
  {
    return new BootstrapErrorBox ().addChild (s);
  }

  @Nonnull
  default BootstrapInfoBox info ()
  {
    return new BootstrapInfoBox ();
  }

  @Nonnull
  default BootstrapInfoBox info (@Nullable final IHCNode aNode)
  {
    return new BootstrapInfoBox ().addChild (aNode);
  }

  @Nonnull
  default BootstrapInfoBox info (@Nullable final String s)
  {
    return new BootstrapInfoBox ().addChild (s);
  }

  @Nonnull
  default BootstrapQuestionBox question ()
  {
    return new BootstrapQuestionBox ();
  }

  @Nonnull
  default BootstrapQuestionBox question (@Nullable final IHCNode aNode)
  {
    return new BootstrapQuestionBox ().addChild (aNode);
  }

  @Nonnull
  default BootstrapQuestionBox question (@Nullable final String s)
  {
    return new BootstrapQuestionBox ().addChild (s);
  }

  @Nonnull
  default BootstrapSuccessBox success ()
  {
    return new BootstrapSuccessBox ();
  }

  @Nonnull
  default BootstrapSuccessBox success (@Nullable final IHCNode aNode)
  {
    return new BootstrapSuccessBox ().addChild (aNode);
  }

  @Nonnull
  default BootstrapSuccessBox success (@Nullable final String s)
  {
    return new BootstrapSuccessBox ().addChild (s);
  }

  @Nonnull
  default BootstrapWarnBox warn ()
  {
    return new BootstrapWarnBox ();
  }

  @Nonnull
  default BootstrapWarnBox warn (@Nullable final IHCNode aNode)
  {
    return new BootstrapWarnBox ().addChild (aNode);
  }

  @Nonnull
  default BootstrapWarnBox warn (@Nullable final String s)
  {
    return new BootstrapWarnBox ().addChild (s);
  }
}
