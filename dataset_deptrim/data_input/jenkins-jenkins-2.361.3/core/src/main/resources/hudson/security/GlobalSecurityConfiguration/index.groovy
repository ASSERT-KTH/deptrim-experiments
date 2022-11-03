package hudson.security.GlobalSecurityConfiguration

import hudson.security.SecurityRealm
import hudson.markup.MarkupFormatterDescriptor
import hudson.security.AuthorizationStrategy
import jenkins.AgentProtocol
import hudson.Functions
import hudson.model.Descriptor

def f=namespace(lib.FormTagLib)
def l=namespace(lib.LayoutTagLib)
def st=namespace("jelly:stapler")

l.layout(permission:app.SYSTEM_READ, title:my.displayName, cssclass:request.getParameter('decorate')) {
    l.side_panel {
        l.tasks {
            l.task(icon: "icon-up icon-md", href: rootURL + '/', title: _("Back to Dashboard"))
            l.task(icon: "icon-gear icon-md", href: "${rootURL}/manage", title: _("Manage Jenkins"))
        }
    }
    l.app_bar(title: my.displayName)

    l.main_panel {
        set("readOnlyMode", !app.hasPermission(app.ADMINISTER))

        p()
        div(class:"behavior-loading") {
            l.spinner(text: _("LOADING"))
        }
        f.form(method:"post",name:"config",action:"configure", class: "jenkins-form") {
            set("instance",my)
            set("descriptor", my.descriptor)

            f.section(title:_("Authentication")) {
                f.entry() {
                    f.checkbox(title:_("Disable remember me"), field: "disableRememberMe")
                }
                f.dropdownDescriptorSelector(title: _("Security Realm"), field: 'securityRealm', descriptors: h.filterDescriptors(app, SecurityRealm.all()))
                f.dropdownDescriptorSelector(title: _("Authorization"), field: 'authorizationStrategy', descriptors: h.filterDescriptors(app, AuthorizationStrategy.all()))
            }

            f.section(title: _("Markup Formatter")) {
                f.dropdownDescriptorSelector(title:_("Markup Formatter"),descriptors: MarkupFormatterDescriptor.all(), field: 'markupFormatter')
            }

            f.section(title: _("Agents")) {
                f.entry(title: _("TCP port for inbound agents"), field: "slaveAgentPort") {
                    if (my.slaveAgentPortEnforced) {
                        if (my.slaveAgentPort == -1) {
                            text(_("slaveAgentPortEnforcedDisabled"))
                        } else if (my.slaveAgentPort == 0) {
                            text(_("slaveAgentPortEnforcedRandom"))
                        } else {
                            text(_("slaveAgentPortEnforced", my.slaveAgentPort))
                        }
                    } else {
                        f.serverTcpPort()
                    }
                }
                f.advanced(title: _("Agent protocols"), align:"left") {
                    f.entry(title: _("Agent protocols")) {
                        def agentProtocols = my.agentProtocols
                        div() {
                            for (AgentProtocol p : AgentProtocol.all()) {
                                if (p.name != null && !p.required) {
                                    f.block() {
                                        f.checkbox(name: "agentProtocol",
                                                title: p.displayName,
                                                checked: agentProtocols.contains(p.name),
                                                json: p.name)
                                    }
                                    div(class: "tr") {
                                        div(class:"setting-description"){
                                            st.include(from:p, page: "description", optional:true)
                                            if (p.deprecated) {
                                              br()
                                              text(b(_("Deprecated. ")))
                                              st.include(from:p, page: "deprecationCause", optional:true)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Functions.getSortedDescriptorsForGlobalConfigByDescriptor(my.FILTER).each { Descriptor descriptor ->
                set("descriptor",descriptor)
                set("instance",descriptor)
                f.rowSet(name:descriptor.jsonSafeClassName) {
                    st.include(from:descriptor, page:descriptor.globalConfigPage)
                }
            }

            l.isAdmin() {
                f.bottomButtonBar {
                    f.submit(value: _("Save"))
                    f.apply()
                }
            }
        }

        l.isAdmin() {
            st.adjunct(includes: "lib.form.confirm")
        }
    }
}

