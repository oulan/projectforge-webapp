/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2012 Kai Reinhard (k.reinhard@micromata.com)
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

package org.projectforge.web.wicket;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.GridBuilder;
import org.projectforge.web.wicket.flowlayout.MyComponentsRepeater;
import org.projectforge.web.wicket.flowlayout.TextPanel;

/**
 * Standard error page should be shown in production mode.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ErrorForm extends AbstractForm<ErrorPageData, ErrorPage>
{
  private static final long serialVersionUID = -637809894879133209L;

  public static final String ONLY4NAMESPACE = "org.projectforge";

  final ErrorPageData data = new ErrorPageData();

  private GridBuilder gridBuilder;

  /**
   * List to create content menu in the desired order before creating the RepeatingView.
   */
  protected MyComponentsRepeater<SingleButtonPanel> actionButtons;

  public ErrorForm(final ErrorPage parentPage)
  {
    super(parentPage);
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();
    addFeedbackPanel();
    final DivPanel errorMessagePanel = new DivPanel("errorMessage");
    add(errorMessagePanel);
    errorMessagePanel.add(new TextPanel(DivPanel.CHILD_ID, new Model<String>() {
      @Override
      public String getObject()
      {
        return parentPage.errorMessage;
      }
    }));
    final RepeatingView repeater = new RepeatingView("flowform");
    add(repeater);
    gridBuilder = newGridBuilder(repeater);
    gridBuilder.newGrid16();
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("feedback.receiver"), true).setNoLabelFor();
      fs.add(new DivTextPanel(fs.newChildId(), data.getReceiver()));
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("feedback.sender"), true).setNoLabelFor();
      fs.add(new DivTextPanel(fs.newChildId(), data.getSender()));
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("errorpage.feedback.messageNumber"), true).setNoLabelFor();
      fs.add(new DivTextPanel(fs.newChildId(), data.getMessageNumber()));
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("errorpage.feedback.description"), true);
      final MaxLengthTextArea description = new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<String>(data, "description"), 4000);
      WicketUtils.setFocus(description);
      fs.add(description).setAutogrow();
    }
    actionButtons = new MyComponentsRepeater<SingleButtonPanel>("buttons");
    add(actionButtons.getRepeatingView());
    {
      final Button cancelButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("cancel")) {
        @Override
        public final void onSubmit()
        {
          parentPage.cancel();
        }
      };
      cancelButton.setDefaultFormProcessing(false); // No validation of the form.
      final SingleButtonPanel callButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), cancelButton, getString("cancel"),
          SingleButtonPanel.CANCEL);
      actionButtons.add(callButtonPanel);
    }
    {
      final Button sendButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("send")) {
        @Override
        public final void onSubmit()
        {
          parentPage.sendFeedback();
        }
      };
      WicketUtils.addTooltip(sendButton, getString("feedback.send.title"));
      final SingleButtonPanel sendButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), sendButton, getString("send"),
          SingleButtonPanel.DEFAULT_SUBMIT);
      actionButtons.add(sendButtonPanel);
      setDefaultButton(sendButton);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractForm#onBeforeRender()
   */
  @Override
  public void onBeforeRender()
  {
    super.onBeforeRender();
    actionButtons.render();
  }
}