import $ from "jquery";
import windowHandle from "window-handle";
import page from "./util/page";
import * as tabBarWidget from "./widgets/config/tabbar";

var isScrolling = false;
var ignoreNextScrollEvent = false;
var pageHeaderHeight = page.pageHeaderHeight();
var breadcrumbBarHeight = page.breadcrumbBarHeight();

// Some stuff useful for testing.
export var tabbars = [];
export var scrollspeed = 500;
// Used to set scrollspeed from the the test suite
export function setScrollspeed(newScrollspeed) {
  scrollspeed = newScrollspeed;
}
var eventListeners = [];
export var on = function (listener) {
  eventListeners.push(listener);
};
function notify(event) {
  for (var i = 0; i < eventListeners.length; i++) {
    eventListeners[i](event);
  }
}

$(function () {
  tabBarWidget.addPageTabs(
    ".config-table.scrollspy",
    function (tabBar) {
      tabbars.push(tabBar);

      tabBarWidget.addFinderToggle(tabBar);
      tabBar.onShowSection(function () {
        // Scroll to the section.
        scrollTo(this, tabBar);
      });

      autoActivateTabs(tabBar);
      page.onWinScroll(function () {
        autoActivateTabs(tabBar);
      });
      page.onWinScroll(function () {
        stickTabbar(tabBar);
      });

      // Manually trigger a repaint, otherwise Folder forms will not position
      // the buttons correctly. This is caused by upgrading jQuery to 3.5.x,
      // and probably has something to do with event listeners running in
      // different order.
      layoutUpdateCallback.call();
    },
    { trackSectionVisibility: true }
  );
});

function scrollTo(section, tabBar) {
  var $header = section.headerRow;
  var scrollTop =
    $header.offset().top -
    ($("#main-panel .jenkins-config-widgets").outerHeight() + 15);

  isScrolling = true;
  $("html,body").animate(
    {
      scrollTop: scrollTop,
    },
    scrollspeed,
    function () {
      if (isScrolling) {
        notify({
          type: "click_scrollto",
          section: section,
        });
        isScrolling = false;
        ignoreNextScrollEvent = stickTabbar(tabBar);
      }
    }
  );
}

/**
 * Watch page scrolling, changing the active tab as needed.
 * @param tabBar The tabbar.
 */
function autoActivateTabs(tabBar) {
  if (isScrolling === true) {
    // Ignore window scroll events while we are doing a scroll.
    // See scrollTo function.
    return;
  }
  if (ignoreNextScrollEvent === true) {
    // Things like repositioning of the tabbar (see stickTabbar)
    // can trigger scroll events that we want to ignore.
    ignoreNextScrollEvent = false;
    return;
  }

  var winScrollTop = page.winScrollTop();
  var sections = tabBar.sections;

  // calculate the top and height of each section to know where to switch the tabs...
  $.each(sections, function (i, section) {
    if (!section.isVisible()) {
      return;
    }

    // each section enters the viewport at its distance down the page, less the height of
    // the toolbar, which hangs down the page. Or it is zero if the section doesn't
    // match or was removed...
    var viewportEntryOffset = section.getViewportEntryOffset();
    // height of this one is the top of the next, less the top of this one.
    var sectionHeight = 0;
    var nextSection = nextVisibleSection(section);
    if (nextSection) {
      sectionHeight =
        nextSection.getViewportEntryOffset() - viewportEntryOffset;
    }

    // the trigger point to change the tab happens when the scroll position passes below the height of the section...
    // ...but we want to wait to advance the tab until the existing section is 75% off the top...
    // ### < 75% ADVANCED
    if (winScrollTop < viewportEntryOffset + 0.75 * sectionHeight) {
      section.markAsActive();
      notify({
        type: "manual_scrollto",
        section: section,
      });
      return false;
    }
  });
}

/**
 * Stick the scrollspy tabbar to the top of the visible area as the user
 * scrolls down the page.
 * @param tabBar The tabbar.
 */
function stickTabbar(tabBar) {
  var win = $(windowHandle.getWindow());
  var winScrollTop = page.winScrollTop();
  var widgetBox = tabBar.configWidgets;
  var configTable = tabBar.configTable;
  var configForm = tabBar.configForm;
  var setWidth = function () {
    widgetBox.width(configForm.outerWidth() - 2);
  };

  if (winScrollTop > pageHeaderHeight - 5) {
    setWidth();
    widgetBox.css({
      position: "fixed",
      top: breadcrumbBarHeight - 5 + "px",
      margin: "0 auto !important",
    });
    configTable.css({ "margin-top": widgetBox.outerHeight() + "px" });
    win.resize(setWidth);
    return true;
  } else {
    widgetBox.removeAttr("style");
    configTable.removeAttr("style");
    win.unbind("resize", setWidth);
    return false;
  }
}

function nextVisibleSection(section) {
  var next = section.getSibling(+1);
  while (next && !next.isVisible()) {
    next = next.getSibling(+1);
  }
  return next;
}
