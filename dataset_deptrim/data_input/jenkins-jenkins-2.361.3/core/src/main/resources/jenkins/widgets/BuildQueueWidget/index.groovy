package jenkins.widgets.BuildQueueWidget

def t = namespace(lib.JenkinsTagLib.class)

t.queue(items:view.queueItems, it:view, filtered:view.filterQueue)
