/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2011 Kai Reinhard (k.reinhard@me.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.core;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.core.SpaceDO;
import org.projectforge.core.SpaceDao;
import org.projectforge.core.SpaceStatus;
import org.projectforge.web.wicket.AbstractAutoLayoutEditPage;
import org.projectforge.web.wicket.AbstractBasePage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;

import de.micromata.genome.gwiki.model.GWikiArtefakt;
import de.micromata.genome.gwiki.model.GWikiElement;
import de.micromata.genome.gwiki.model.GWikiElementInfo;
import de.micromata.genome.gwiki.model.GWikiPropKeys;
import de.micromata.genome.gwiki.model.GWikiProps;
import de.micromata.genome.gwiki.model.GWikiSettingsProps;
import de.micromata.genome.gwiki.model.GWikiWebUtils;
import de.micromata.genome.gwiki.page.GWikiContext;
import de.micromata.genome.gwiki.page.GWikiStandaloneContext;
import de.micromata.genome.gwiki.page.impl.GWikiWikiPageBaseArtefakt;

@EditPage(defaultReturnPage = SpaceListPage.class)
public class SpaceEditPage extends AbstractAutoLayoutEditPage<SpaceDO, SpaceEditForm, SpaceDao>
{
  private static final long serialVersionUID = 941381326473678282L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SpaceEditPage.class);

  @SpringBean(name = "spaceDao")
  private SpaceDao spaceDao;

  public SpaceEditPage(final PageParameters parameters)
  {
    super(parameters, "space");
    init();
    if (isNew() == true) {
      getData().setStatus(SpaceStatus.ACTIVE);
    }
  }

  @Override
  protected SpaceDao getBaseDao()
  {
    return spaceDao;
  }

  @Override
  protected SpaceEditForm newEditForm(final AbstractEditPage< ? , ? , ? > parentPage, final SpaceDO data)
  {
    return new SpaceEditForm(this, data);
  }

  /**
   * Removes a GWiki-Space recursivley
   * 
   * @see org.projectforge.web.wicket.AbstractEditPage#onDelete()
   */
  @Override
  public AbstractBasePage onDelete()
  {
    final GWikiStandaloneContext wikiContext = GWikiStandaloneContext.create();
    final String pageId = GWikiContext.getPageIdFromTitle(getForm().getData().getIdentifier()) + "/Index";

    final GWikiElement element = wikiContext.getWikiWeb().findElement(pageId);

    if (element != null) {
      removeElementChildren(wikiContext, element.getElementInfo());
    }
    
    // TODO (cclaus) TextExtracts and TextIndex Fragments are still present after deleting!

    return super.onDelete();
  }

  /**
   * Recursive function to remove all element children, before the element itself will be removed.
   * 
   * @param wikiContext
   * @param elementInfo
   */
  private void removeElementChildren(final GWikiContext wikiContext, final GWikiElementInfo elementInfo)
  {
    /*
     * TODO (cclaus) check rights necessary?
     * 
     * if (wikiContext.getWikiWeb().getAuthorization().isAllowToEdit(wikiContext, elementInfo) == false) { return; }
     */
    final List<GWikiElementInfo> cl = wikiContext.getElementFinder().getAllDirectChilds(elementInfo);
    for (final GWikiElementInfo childElementInfo : cl) {
      removeElementChildren(wikiContext, childElementInfo);
    }

    final GWikiElement element = wikiContext.getWikiWeb().findElement(elementInfo.getId());

    wikiContext.getWikiWeb().removeWikiPage(wikiContext, element);
  }

  /**
   * Creates a gwiki page: /gwiki/<SPACENAME>/Index if the space doesn't exist Otherwise the corresponding space will be updated by the PF
   * configuration dialog.
   * 
   * @see org.projectforge.web.wicket.AbstractEditPage#onSaveOrUpdate()
   */
  @Override
  public AbstractBasePage onSaveOrUpdate()
  {
    final SpaceDO spaceData = getForm().getData();
    final GWikiStandaloneContext wikiContext = GWikiStandaloneContext.create();
    final String metaTemplateId = "admin/templates/StandardWikiPageMetaTemplate";
    final String pageId = GWikiContext.getPageIdFromTitle(spaceData.getIdentifier()) + "/Index";
    final String pageIntro = "{pageintro}";

    // TODO: (cclaus) check rights necessary?

    /*
     * Test, if pageId already exists. If not, a new space will be created
     */
    if (wikiContext.getWikiWeb().findElement(pageId) == null) {
      // Creates GWiki Properties
      final GWikiProps props = new GWikiSettingsProps();
      props.setStringValue(GWikiPropKeys.WIKIMETATEMPLATE, metaTemplateId);
      props.setStringValue(GWikiPropKeys.TYPE, metaTemplateId);
      props.setStringValue(GWikiPropKeys.PARENTPAGE, "Index");
      props.setStringValue(GWikiPropKeys.TITLE, spaceData.getTitle());

      // Creates new GWiki Element
      final GWikiElement element = GWikiWebUtils.createNewElement(wikiContext, pageId, metaTemplateId, spaceData.getTitle());
      element.getElementInfo().setProps(props);

      // Gets GWiki 'MainPage'. All wiki content will be stored in this artifact
      final GWikiArtefakt< ? > part = element.getPart("MainPage");

      // Tests if the 'MainPage' is a GWikiWikiPage
      if (part != null && part instanceof GWikiWikiPageBaseArtefakt) {
        final GWikiWikiPageBaseArtefakt art = (GWikiWikiPageBaseArtefakt) part;
        final StringBuilder content = new StringBuilder();

        // Appends {pageIntro} description {pageIntro}
        if (StringUtils.isNotEmpty(spaceData.getDescription())) {
          content.append(pageIntro).append(spaceData.getDescription());
          content.append(pageIntro).append("\n\n");
        }

        // Appends children macro
        content.append("{children:depth=1}");

        // Sets storage data to the GWiki artifact
        art.setStorageData(content.toString());
        art.compileFragements(wikiContext);
      }

      // saves Element and updates page caches
      wikiContext.getWikiWeb().saveElement(wikiContext, element, false);
    }

    /*
     * In update case: updates element information and description content
     */
    else {
      final GWikiElement element = wikiContext.getWikiWeb().findElement(pageId);

      final GWikiProps props = element.getElementInfo().getProps();
      props.setStringValue(GWikiPropKeys.PAGEID, spaceData.getIdentifier());
      props.setStringValue(GWikiPropKeys.TITLE, spaceData.getTitle());

      final GWikiArtefakt< ? > part = element.getPart("MainPage");

      // Gets the Wiki code and updates it's description when needed
      if (part != null && part instanceof GWikiWikiPageBaseArtefakt) {
        final GWikiWikiPageBaseArtefakt art = (GWikiWikiPageBaseArtefakt) part;

        final StringBuilder builder = new StringBuilder(art.getStorageData());
        final int startIndex = builder.indexOf(pageIntro) + pageIntro.length();
        final int endIndex = builder.indexOf(pageIntro, startIndex);

        if (StringUtils.isNotEmpty(spaceData.getDescription())) {
          if (startIndex != 10) {
            builder.delete(startIndex, endIndex);
            builder.insert(startIndex, spaceData.getDescription());
          } else {
            builder.insert(0, pageIntro + spaceData.getDescription() + pageIntro);
          }

        } else {
          builder.delete(0, endIndex + pageIntro.length());
        }

        art.setStorageData(builder.toString());
        art.compileFragements(wikiContext);
      }

      wikiContext.getWikiWeb().saveElement(wikiContext, element, false);
    }

    return super.onSaveOrUpdate();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

}
