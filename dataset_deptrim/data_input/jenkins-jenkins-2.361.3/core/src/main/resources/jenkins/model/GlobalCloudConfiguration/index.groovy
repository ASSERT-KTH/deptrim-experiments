package jenkins.model.GlobalCloudConfiguration

import hudson.slaves.Cloud
import jenkins.model.Jenkins


def f = namespace(lib.FormTagLib)
def l = namespace(lib.LayoutTagLib)
def st = namespace("jelly:stapler")

l.layout(norefresh:true, permission:app.SYSTEM_READ, title:my.displayName) {
    set("readOnlyMode", !app.hasPermission(app.ADMINISTER))
    l.side_panel {
        l.tasks {
            l.task(icon:"icon-up icon-md", href:rootURL+'/', title:_("Back to Dashboard"))
            l.task(icon:"symbol-settings", href: "../computer/", title:_("Manage Nodes"))
        }
    }
    l.app_bar(title: my.displayName)
    l.main_panel {
        def clouds = Cloud.all()
        if (!clouds.isEmpty()) {
            p()
            div(class:"behavior-loading") {
                l.spinner(text: _("LOADING"))
            }

            f.form(method:"post",name:"config",action:"configure", class: "jenkins-form") {
                f.block {
                    if (app.clouds.size() == 0 && !h.hasPermission(app.ADMINISTER)) {
                        p(_("No clouds have been configured."))
                    }

                    f.hetero_list(name:"cloud", hasHeader:true, descriptors:Cloud.all(), items:app.clouds,
                            addCaption:_("Add a new cloud"), deleteCaption:_("Delete cloud"))
                }

                l.isAdmin {
                    f.bottomButtonBar {
                        f.submit(value: _("Save"))
                        f.apply(value: _("Apply"))
                    }
                }
            }
            l.isAdmin {
                st.adjunct(includes: "lib.form.confirm")
            }
        } else {
            String label = Jenkins.get().updateCenter.getCategoryDisplayName("cloud")

            p(_("There are no cloud implementations for dynamically allocated agents installed. "))
            a(href: rootURL + "/pluginManager/available?filter=" + URLEncoder.encode(label, "UTF-8"), _("Go to plugin manager."))
        }
    }
}
