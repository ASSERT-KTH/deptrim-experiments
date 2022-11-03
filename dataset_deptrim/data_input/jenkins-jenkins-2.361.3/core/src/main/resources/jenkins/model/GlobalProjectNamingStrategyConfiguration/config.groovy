package jenkins.model.GlobalProjectNamingStrategyConfiguration

import jenkins.model.ProjectNamingStrategy

def f=namespace(lib.FormTagLib)

div(class: "jenkins-form-item") {
    f.optionalBlock( field:"useProjectNamingStrategy", title:_("useNamingStrategy"), checked:app.useProjectNamingStrategy) {

        f.entry(title:_("namingStrategyTitle")) {
            div(style:"width:100%") {
                f.descriptorRadioList(title:_("strategy"), varName:"namingStrategy", instance:app.projectNamingStrategy, descriptors:ProjectNamingStrategy.all())
            }
        }

    }
}
