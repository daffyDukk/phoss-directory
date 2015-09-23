/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.pyp.publisher.app.pub;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.css.property.CCSSProperties;
import com.helger.html.css.DefaultCSSClassProvider;
import com.helger.html.css.ICSSClassProvider;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.IHCElement;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCP;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.metadata.HCHead;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.photon.basic.app.menu.ApplicationMenuTree;
import com.helger.photon.basic.app.menu.IMenuItemExternal;
import com.helger.photon.basic.app.menu.IMenuItemPage;
import com.helger.photon.basic.app.menu.IMenuObject;
import com.helger.photon.basic.app.menu.IMenuSeparator;
import com.helger.photon.basic.app.menu.IMenuTree;
import com.helger.photon.basic.app.menu.MenuItemDeterminatorCallback;
import com.helger.photon.basic.security.AccessManager;
import com.helger.photon.basic.security.login.LoggedInUserManager;
import com.helger.photon.basic.security.user.IUser;
import com.helger.photon.basic.security.util.SecurityHelper;
import com.helger.photon.bootstrap3.CBootstrapCSS;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.base.BootstrapContainer;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.dropdown.BootstrapDropdownMenu;
import com.helger.photon.bootstrap3.nav.BootstrapNav;
import com.helger.photon.bootstrap3.navbar.BootstrapNavbar;
import com.helger.photon.bootstrap3.navbar.EBootstrapNavbarPosition;
import com.helger.photon.bootstrap3.navbar.EBootstrapNavbarType;
import com.helger.photon.bootstrap3.pages.settings.SystemMessageUIHelper;
import com.helger.photon.bootstrap3.uictrls.ext.BootstrapMenuItemRenderer;
import com.helger.photon.bootstrap3.uictrls.ext.BootstrapMenuItemRendererHorz;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.photon.core.app.layout.CLayout;
import com.helger.photon.core.app.layout.ILayoutAreaContentProvider;
import com.helger.photon.core.app.redirect.ForcedRedirectManager;
import com.helger.photon.core.servlet.AbstractSecureApplicationServlet;
import com.helger.photon.core.servlet.LogoutServlet;
import com.helger.photon.core.url.LinkHelper;
import com.helger.photon.uicore.page.IWebPage;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.pyp.publisher.app.AppCommonUI;
import com.helger.pyp.publisher.app.CApp;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * The viewport renderer (menu + content area)
 *
 * @author Philip Helger
 */
public final class AppRendererPublic implements ILayoutAreaContentProvider <LayoutExecutionContext>
{
  private static final ICSSClassProvider CSS_CLASS_FOOTER_LINKS = DefaultCSSClassProvider.create ("footer-links");

  private final List <IMenuObject> m_aFooterObjects;

  public AppRendererPublic ()
  {
    m_aFooterObjects = new ArrayList <IMenuObject> ();
    ApplicationMenuTree.getTree ().iterateAllMenuObjects (aCurrentObject -> {
      if (aCurrentObject.containsAttribute (CMenuPublic.FLAG_FOOTER))
        m_aFooterObjects.add (aCurrentObject);
    });
  }

  private static void _addNavbarLoginLogout (@Nonnull final LayoutExecutionContext aLEC,
                                             @Nonnull final BootstrapNavbar aNavbar)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();
    final IUser aUser = LoggedInUserManager.getInstance ().getCurrentUser ();
    if (aUser != null)
    {
      if (AccessManager.getInstance ().hasUserRole (aUser.getID (), CApp.ROLE_CONFIG_ID))
      {
        aNavbar.addButton (EBootstrapNavbarPosition.COLLAPSIBLE_DEFAULT,
                           new BootstrapButton ().addChild ("Goto secure area")
                                                 .setOnClick (LinkHelper.getURLWithContext (AbstractSecureApplicationServlet.SERVLET_DEFAULT_PATH +
                                                                                            "/")));
      }

      final Locale aDisplayLocale = aLEC.getDisplayLocale ();
      final BootstrapNav aNav = new BootstrapNav ();

      aNav.addItem (new HCSpan ().addClass (CBootstrapCSS.NAVBAR_TEXT)
                                 .addChild ("Welcome ")
                                 .addChild (new HCStrong ().addChild (SecurityHelper.getUserDisplayName (aUser,
                                                                                                         aDisplayLocale))));
      aNav.addItem (new HCA (LinkHelper.getURLWithContext (aRequestScope,
                                                           LogoutServlet.SERVLET_DEFAULT_PATH)).addChild (EPhotonCoreText.LOGIN_LOGOUT.getDisplayText (aDisplayLocale)));
      aNavbar.addNav (EBootstrapNavbarPosition.COLLAPSIBLE_RIGHT, aNav);
    }
    else
    {
      final BootstrapNav aNav = new BootstrapNav ();
      final BootstrapDropdownMenu aDropDown = aNav.addDropdownMenu ("Login");
      {
        // 300px would lead to a messy layout - so 250px is fine
        final HCDiv aDiv = new HCDiv ().addStyle (CCSSProperties.PADDING.newValue ("10px"))
                                       .addStyle (CCSSProperties.WIDTH.newValue ("250px"));
        aDiv.addChild (AppCommonUI.createViewLoginForm (aLEC, null, false).addClass (CBootstrapCSS.NAVBAR_FORM));
        aDropDown.addItem (aDiv);
      }
      aNavbar.addNav (EBootstrapNavbarPosition.COLLAPSIBLE_LEFT, aNav);
    }
  }

  @Nonnull
  private static BootstrapNavbar _getNavbar (@Nonnull final LayoutExecutionContext aLEC)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final ISimpleURL aLinkToStartPage = aLEC.getLinkToMenuItem (aLEC.getMenuTree ().getDefaultMenuItemID ());

    final BootstrapNavbar aNavbar = new BootstrapNavbar (EBootstrapNavbarType.STATIC_TOP, true, aDisplayLocale);
    ((BootstrapContainer) aNavbar.getContainer ()).setFluid (true);
    aNavbar.addBrand (new HCSpan ().addClass (AppCommonUI.CSS_CLASS_LOGO1).addChild (CApp.getApplicationTitle ()),
                      aLinkToStartPage);

    _addNavbarLoginLogout (aLEC, aNavbar);
    return aNavbar;
  }

  @Nonnull
  public static IHCNode getMenuContent (@Nonnull final LayoutExecutionContext aLEC)
  {
    // Main menu
    final IMenuTree aMenuTree = aLEC.getMenuTree ();
    final MenuItemDeterminatorCallback aCallback = new MenuItemDeterminatorCallback (aMenuTree,
                                                                                     aLEC.getSelectedMenuItemID ())
    {
      @Override
      protected boolean isMenuItemValidToBeDisplayed (@Nonnull final IMenuObject aMenuObj)
      {
        // Don't show items that belong to the footer
        if (aMenuObj.containsAttribute (CMenuPublic.FLAG_FOOTER))
          return false;

        // Use default code
        return super.isMenuItemValidToBeDisplayed (aMenuObj);
      }
    };
    final IHCElement <?> aMenu = BootstrapMenuItemRenderer.createSideBarMenu (aLEC, aCallback);
    return aMenu;
  }

  @SuppressWarnings ("unchecked")
  @Nonnull
  public static IHCNode getMainContent (@Nonnull final LayoutExecutionContext aLEC)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();

    // Get the requested menu item
    final IMenuItemPage aSelectedMenuItem = aLEC.getSelectedMenuItem ();

    // Resolve the page of the selected menu item (if found)
    IWebPage <WebPageExecutionContext> aDisplayPage;
    if (aSelectedMenuItem.matchesDisplayFilter ())
    {
      // Only if we have display rights!
      aDisplayPage = (IWebPage <WebPageExecutionContext>) aSelectedMenuItem.getPage ();
    }
    else
    {
      // No rights -> goto start page
      aDisplayPage = (IWebPage <WebPageExecutionContext>) aLEC.getMenuTree ().getDefaultMenuItem ().getPage ();
    }

    final WebPageExecutionContext aWPEC = new WebPageExecutionContext (aLEC, aDisplayPage);

    // Build page content: header + content
    final HCNodeList aPageContainer = new HCNodeList ();

    // First add the system message
    aPageContainer.addChild (SystemMessageUIHelper.createDefaultBox ());

    // Handle 404 case here (see error404.jsp)
    if ("true".equals (aRequestScope.getAttributeAsString ("httpError")))
    {
      final String sHttpStatusCode = aRequestScope.getAttributeAsString ("httpStatusCode");
      final String sHttpStatusMessage = aRequestScope.getAttributeAsString ("httpStatusMessage");
      final String sHttpRequestURI = aRequestScope.getAttributeAsString ("httpRequestUri");
      aPageContainer.addChild (new BootstrapErrorBox ().addChild ("HTTP error " +
                                                                  sHttpStatusCode +
                                                                  " (" +
                                                                  sHttpStatusMessage +
                                                                  ")" +
                                                                  (StringHelper.hasText (sHttpRequestURI) ? " for request URI " +
                                                                                                            sHttpRequestURI
                                                                                                          : "")));
    }
    else
    {
      // Add the forced redirect content here
      if (aWPEC.containsAttribute (ForcedRedirectManager.REQUEST_PARAMETER_PRG_ACTIVE))
        aPageContainer.addChild (ForcedRedirectManager.getLastForcedRedirectContent (aDisplayPage.getID ()));
    }

    // Add page header
    aPageContainer.addChild (aDisplayPage.getHeaderNode (aWPEC));

    // Main fill page content
    aDisplayPage.getContent (aWPEC);

    // Add page content to result
    aPageContainer.addChild (aWPEC.getNodeList ());
    return aPageContainer;
  }

  @Nonnull
  public IHCNode getContent (@Nonnull final LayoutExecutionContext aLEC, @Nonnull final HCHead aHead)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final HCNodeList ret = new HCNodeList ();

    // Header
    ret.addChild (_getNavbar (aLEC));

    final BootstrapContainer aOuterContainer = ret.addAndReturnChild (new BootstrapContainer ().setFluid (false));

    // Content - no menu
    aOuterContainer.addChild (getMainContent (aLEC));

    // Footer
    {
      final BootstrapContainer aDiv = new BootstrapContainer ().setFluid (true).setID (CLayout.LAYOUT_AREAID_FOOTER);

      aDiv.addChild (new HCP ().addChild ("PYP Publisher based on ")
                               .addChild (new HCA ("https://github.com/phax/ph-oton").addChild ("ph-oton"))
                               .addChild (" stack"));
      aDiv.addChild (new HCP ().addChild ("Sources are available on ")
                               .addChild (new HCA ("https://github.com/phax/peppol-yellow-pages").addChild ("GitHub")));
      aDiv.addChild (new HCP ().addChild ("Created by ")
                               .addChild (new HCA ("https://github.com/phax").addChild ("Philip Helger"))
                               .addChild (" for PEPPOL - Twitter: ")
                               .addChild (new HCA ("https://twitter.com/philiphelger").addChild ("@philiphelger")));

      final BootstrapMenuItemRendererHorz aRenderer = new BootstrapMenuItemRendererHorz (aDisplayLocale);
      final HCUL aUL = aDiv.addAndReturnChild (new HCUL ().addClass (CSS_CLASS_FOOTER_LINKS));
      for (final IMenuObject aMenuObj : m_aFooterObjects)
      {
        if (aMenuObj instanceof IMenuSeparator)
          aUL.addItem (aRenderer.renderSeparator (aLEC, (IMenuSeparator) aMenuObj));
        else
          if (aMenuObj instanceof IMenuItemPage)
            aUL.addItem (aRenderer.renderMenuItemPage (aLEC, (IMenuItemPage) aMenuObj, false, false, false));
          else
            if (aMenuObj instanceof IMenuItemExternal)
              aUL.addItem (aRenderer.renderMenuItemExternal (aLEC, (IMenuItemExternal) aMenuObj, false, false, false));
            else
              throw new IllegalStateException ("Unsupported menu object type!");
      }
      ret.addChild (aDiv);
    }

    return ret;
  }
}
