if [ -x '/usr/libexec/java_home' ]; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 1.8*)
    export JDK_HOME=$JAVA_HOME
elif [ -d /opt/java ]; then
    export JAVA_HOME=/opt/java
    export JDK_HOME=$JAVA_HOME
fi

if [ -f "/usr/local/etc/bash_completion.d/git-completion.bash" ] ; then
    source /usr/local/etc/bash_completion.d/git-completion.bash
fi

if [ -f "/usr/local/etc/bash_completion.d/git-prompt.sh" ] ; then
    source /usr/local/etc/bash_completion.d/git-prompt.sh
elif [ -f '/etc/bash_completion.d/git' ]; then
    source '/etc/bash_completion.d/git'
else
    echo "Cannot find Git completion"
fi

export HISTFILESIZE=2500
export EDITOR='vim'
export GREP_OPTIONS='--color=always'

PS1="\[\033[33m\]\$(__git_ps1)\[\033[34m\] \W\[\033[32m\] \$ \[\033[0m\]"

export PATH=$HOME/bin:/opt/X11/bin:/usr/local/bin:/usr/local/sbin:$JAVA_HOME/bin:$PATH
export MAVEN_OPTS="-Xmx1024m -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC"

if [ -d '/Applications/Flash Player.app/Contents/MacOS' ]; then
    export FLASH_PLAYER=/Applications/Flash\ Player.app/Contents/MacOS
    export PATH=$PATH:$FLASH_PLAYER
fi
if [ -d '/usr/local/jrebel' ]; then
    export REBEL_HOME=/usr/local/jrebel
    # export MAVEN_OPTS="$MAVEN_OPTS -noverify -javaagent:$REBEL_HOME/jrebel.jar"
fi

function mvn {
    if [ -z $(which mvn 2>/dev/null) ]; then
        echo "Cannot find Maven on path"
        return 1
    else
        $(which mvn) "$@"
        MVN_EXIT=$?
        if [ -n "$(which growlnotify 2>/dev/null)" ]; then
            if [ $MVN_EXIT == 0 ]; then
                growlnotify -n Maven -m "Maven build successful at $(date)"
            else
                growlnotify -n Maven -m "Maven build failed at $(date)"
            fi
        fi
        return $MVN_EXIT
    fi
}

# RVM
if [ -f $HOME/.rvm/scripts/rvm ]; then
    source $HOME/.rvm/scripts/rvm
fi

alias git-log='git log --graph --pretty="format:%C(yellow)%h%Cblue%d%Creset %s %C(green)(%ar) %C(cyan)%an%Creset"'
alias gl='git-log'
alias gd='git diff'
alias gs='git status'
alias gf='git fetch'
alias gr='git rebase'
alias grom='git rebase origin/master'
alias gpu='git push'
alias gpff='git pull --ff-only'
alias gpr='git pull --rebase'
alias gst='git stash'
alias gm='git merge'
alias gcp='git cherry-pick'

alias mci='mvn clean install'
alias mcint='mvn clean install -DskipTests -Dmaven.test.skip=true'

alias sshvm='ssh -i $HOME/Projects/support/deployment/config/deployment-key -p 5123 $(whoami)-centos.london.yazino.com'

function denv { # type, env_name, typecomponents...
    local TYPE=$1
    shift
    local ENV=$1
    shift
    pushd $HOME/Projects/support/deployment > /dev/null
    bin/deploy -e=$ENV -d=$TYPE -k -c $@
    local EXIT_CODE=$?
    popd > /dev/null
    return $EXIT_CODE
}

function penv { # env_name, components...
    local ENV=$1
    shift
    pushd $HOME/Projects/support/deployment > /dev/null
    bin/deploy -e=$ENV -p=error $@
    local EXIT_CODE=$?
    popd > /dev/null
    return $EXIT_CODE
}

alias ddev="denv dev"
alias dlocal="ddev"
alias dlast="denv last-release"
alias dvm="ddev $(whoami)-centos"
alias pvm="penv $(whoami)-centos"

# Load external profiles
BASH_FILES=$(ls $HOME/.bash_profile_* 2> /dev/null | wc -l)
if [ $BASH_FILES != '0' ]; then
    for FILE in $HOME/.bash_profile_*; do
        source $FILE
    done
fi

alias cdstrata='cd ~/Projects/strata/strata/strata.server'
alias cdplatform='cd ~/Projects/platform/'
alias cdsupport='cd ~/Projects/support/'
alias cdmarketing='cd ~/Projects/marketing/marketing-api/'
alias cdbi='cd ~/Projects/bi/'
alias cdbi-redshift='cd ~/Projects/bi/bi-redshift/'
