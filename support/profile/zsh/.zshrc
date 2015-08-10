# Path to your oh-my-zsh configuration.
ZSH="$HOME/.oh-my-zsh"

# Set name of the theme to load.
# Look in ~/.oh-my-zsh/themes/
# Optionally, if you set this to "random", it'll load a random theme each
# time that oh-my-zsh is loaded.
# ZSH_THEME="robbyrussell"
# ZSH_THEME="agnoster"
# ZSH_THEME="norm"
ZSH_THEME="yazino" # A similar theme to our bash profile, but you'll need my branch of oh-my-zsh

# Set to this to use case-sensitive completion
# CASE_SENSITIVE="true"

# Comment this out to disable bi-weekly auto-update checks
DISABLE_AUTO_UPDATE="true"

# Uncomment to change how many often would you like to wait before auto-updates occur? (in days)
# export UPDATE_ZSH_DAYS=13

# Uncomment following line if you want to disable colors in ls
# DISABLE_LS_COLORS="true"

# Uncomment following line if you want to disable autosetting terminal title.
# DISABLE_AUTO_TITLE="true"

# Uncomment following line if you want red dots to be displayed while waiting for completion
COMPLETION_WAITING_DOTS="true"

# Which plugins would you like to load? (plugins can be found in ~/.oh-my-zsh/plugins/*)
# Custom plugins may be added to ~/.oh-my-zsh/custom/plugins/
# Example format: plugins=(rails git textmate ruby lighthouse)
plugins=(brew git history-substring-search mvn osx ruby rvm sublime)

if [ -f "$ZSH/oh-my-zsh.sh" ]; then
    source $ZSH/oh-my-zsh.sh
else
    echo "WARNING! Can't find oh-my-zsh. Please run: git clone git://github.com/robbyrussell/oh-my-zsh.git $ZSH"
    echo "You can also clone git://github.com/jshiell/oh-my-zsh.git if you want a theme close to that of the Bash profile (infernus)"
fi

# Customize to your needs...

if [ -x '/usr/libexec/java_home' ]; then
    export JAVA_HOME=$(/usr/libexec/java_home -v '1.8*')
    export JDK_HOME=$JAVA_HOME
fi

export EDITOR='vim'

# Needed where /tmp and homebrew are on different volumes
if [ -f '/usr/local/bin/brew' -a ! -d '/usr/local/var/tmp' ]; then
    mkdir -p /usr/local/var/tmp
fi
export HOMEBREW_TEMP=/usr/local/var/tmp

export PATH=$HOME/bin:/usr/local/bin:/usr/local/sbin:/opt/X11/bin:$JAVA_HOME/bin:$PATH

# RVM
if [ -f $HOME/.rvm/scripts/rvm ]; then
    source $HOME/.rvm/scripts/rvm
fi

export MAVEN_OPTS="-Xmx1024m -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC"

if [ -f "$HOME/Projects/bi/bi-reporting/src/main/resources/psql-credentials" ]; then
    export PGPASSFILE=$HOME/Projects/bi/bi-reporting/src/main/resources/psql-credentials
    chmod 0600 $PGPASSFILE
fi

unset M2_HOME

if [ -d "/usr/local/android-sdk-macosx" ]; then
    export ANDROID_HOME=/usr/local/android-sdk-macosx
elif [ -d "/Applications/android-sdk-macosx" ]; then
    export ANDROID_HOME=/Applications/android-sdk-macosx
fi

if [ -d '/Applications/Flash Player Debugger.app/Contents/MacOS' ]; then
    export FLASH_PLAYER_HOME=/Applications/Flash\ Player\ Debugger.app/Contents/MacOS
    export PATH=$PATH:$FLASH_PLAYER_HOME
elif [ -d '/Applications/Flash Player.app/Contents/MacOS' ]; then
    export FLASH_PLAYER=/Applications/Flash\ Player.app/Contents/MacOS
    export PATH=$PATH:$FLASH_PLAYER
fi
if [ -d '/usr/local/jrebel' ]; then
    export REBEL_HOME=/usr/local/jrebel
    # export MAVEN_OPTS="$MAVEN_OPTS -noverify -javaagent:$REBEL_HOME/jrebel.jar"
fi

function mvn {
    MVN=/usr/local/bin/mvn
    if [[ ! -x $MVN ]]; then
        echo "Cannot find Maven in /usr/local/bin"
        return 1
    else
        colour_maven "$@"
        MVN_EXIT=$?
        if [[ -n $(which terminal-notifier 2>/dev/null) ]]; then
            if [[ $MVN_EXIT == 0 ]]; then
                terminal-notifier -title Maven -message "Maven build successful at $(date)" 2>/dev/null
            else
                terminal-notifier -title Maven -message "Maven build failed at $(date)" 2>/dev/null
            fi
        fi
        return $MVN_EXIT
    fi
}

alias git-log='git log --graph --pretty="format:%C(yellow)%h%Cblue%d%Creset %s %C(green)(%ar) %C(cyan)%an%Creset"'
alias gl='git-log'
alias gd='git diff'
alias gwd="git diff --color-words --word-diff-regex='[^[:space:]]|([[:alnum:]]|UTF_8_GUARD)+'"
alias gs='git status -sb'
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

ssh_cleanup() {
    echo -e "\033]50;SetProfile=Default\a"
}

ssh() {
    trap ssh_cleanup EXIT
    echo -e "\033]50;SetProfile=SSH\a"
    /usr/bin/ssh $@
}

function denv { # type, env_name, typecomponents...
    local TYPE=$1
    shift
    local ENV=$1
    shift
    local CURRENT_DIR=$(dirs -v | head -n 1 | awk '{print $2}')
    if [ "$CURRENT_DIR" != "~/Projects/support/deployment" ]; then
        pushd $HOME/Projects/support/deployment >/dev/null
    fi
    bin/deploy -e=$ENV -d=$TYPE -k -c $@
    local EXIT_CODE=$?
    if [ "$CURRENT_DIR" != "~/Projects/support/deployment" ]; then
        popd >/dev/null
    fi
    return $EXIT_CODE
}

function penv { # env_name, components...
    local ENV=$1
    shift
    local CURRENT_DIR=$(dirs -v | head -n 1 | awk '{print $2}')
    if [ "$CURRENT_DIR" != "~/Projects/support/deployment" ]; then
        pushd $HOME/Projects/support/deployment >/dev/null
    fi
    bin/deploy -e=$ENV -p=error $@
    local EXIT_CODE=$?
    if [ "$CURRENT_DIR" != "~/Projects/support/deployment" ]; then
        popd >/dev/null
    fi
    return $EXIT_CODE
}

function dirty {
    local CURRENT_DIR=$(dirs -v | head -n 1 | awk '{print $2}')
    if [ "$CURRENT_DIR" != "~/Projects" ]; then
        pushd $HOME/Projects >/dev/null
    fi
    for DIR in $(ls .); do
        if [ -d "$DIR/.git" ]; then
            pushd $DIR >/dev/null
            if [ -z "$(git status | grep 'nothing to commit, working directory clean')" ]; then
                echo "$DIR is dirty"
            elif [ -n "$(git status | grep -E 'by \d+ commit')" ]; then
                echo "$DIR is ahead/behind of origin"
            fi
            popd >/dev/null
        fi
    done
    if [ "$CURRENT_DIR" != "~/Projects" ]; then
        popd >/dev/null
    fi
}

alias ddev="denv dev"
alias dlocal="ddev"
alias dlast="denv last-release"
alias dvm="ddev $(whoami)-centos --skip-chef"
alias pvm="penv $(whoami)-centos"

for FILE in $(find ~ -name '.zshrc_*' -print -maxdepth 1); do
    source "$FILE"
done

# Maven colour - taken from https://gist.github.com/1881211, modified for zshrc/our env

xterm_color() {
    c_bold=`tput setaf 0`
    c_bg_bold=`tput setab 0`
    c_black=`tput setab 0`
    c_bg_black=`tput setab 0`
    c_cyan=`tput setaf 6`
    c_bg_cyan=`tput setab 6`
    c_magenta=`tput setaf 5`
    c_bg_magenta=`tput setab 5`
    c_red=`tput setaf 1`
    c_bg_red=`tput setab 1`
    c_white=`tput setaf 7`
    c_bg_white=`tput setab 7`
    c_green=`tput setaf 2`
    c_bg_green=`tput setab 2`
    c_yellow=`tput setaf 3`
    c_bg_yellow=`tput setab 3`
    c_blue=`tput setaf 4`
    c_bg_blue=`tput setab 4`
    c_end=`tput sgr0`
}

colour_maven() {
    local MVN=/usr/local/bin/mvn
    if [[ ! -x $MVN ]]; then
        echo "Cannot find Maven in /usr/local/bin"
        return 1
    fi

    # pick color type
    if [ $TERM = 'xterm' -o $TERM = 'xterm-color' -o $TERM = 'xterm-256color' ]; then
        xterm_color
    else
        echo "${c_red}WARNING:::Terminal '${TERM}' is not supported at this time. Colourised output will not happen for Maven${c_end}"
    fi

    error=${c_bold}${c_red}
    info=${c_end}
    warn=${c_yellow}
    success=${c_green}
    projectname=${c_bold}${c_cyan}
    skipped=${c_white}
    downloading=${c_magenta}

    $MVN $* | sed -e "s/\(\[INFO\]\) Building\( .*\)/${info}\1${projectname}\2${c_end}/g" \
        -e "s/\(Time elapsed: \)\([0-9]+[.]*[0-9]*.sec\)/${c_cyan}\1${c_white}\2${c_end}/g" \
        -e "s/\(Downloading: .*\)/${downloading}\1${c_end}/g" \
        -e "s/BUILD FAILURE/${error}BUILD FAILURE${c_end}/g" \
        -e "s/WARNING: \([a-zA-Z0-9.-/\\ :]+\)/${warn}WARNING: \1${c_end}/g" \
        -e "s/SEVERE: \(.+\)/${c_white}${c_bg_red}SEVERE: \1${c_end}/g" \
        -e "s/Caused by: \(.+\)/${c_white}${c_bg_green}Caused by: \1${c_end}/g" \
        -e "s/Running \(.+\)/${c_green}Running \1${c_end}/g" \
        -e "s/FAILURE \(\[[0-9]+.[:0-9]+s\]\)/${error}FAILURE \1${c_end}/g" \
        -e "s/SUCCESS \(\[[0-9]+.[:0-9]+s\]\)/${success}SUCCESS \1${c_end}/g" \
        -e "s/\(\[INFO.*\)/${info}\1${c_end}/g" \
        -e "s/INFO: \(.+\)/${c_white}INFO: \1${c_end}/g" \
        -e "s/\(\[WARN.*\)/${warn}\1${c_end}/g" \
        -e "s/\(\[ERROR.*\)/${error}\1${c_end}/g" \
        -e "s/\(<<< FAILURE!\)/${error}\1${c_end}/g" \
        -e "s/Tests run: \([0-9]*\), Failures: \([0-9]*\), Errors: \([0-9]*\), Skipped: \([0-9]*\)/${c_green}Tests run: \1 ${c_end}, Failures: ${warn}\2 ${c_end}, Errors: ${error}\3 ${c_end}, Skipped:  ${skipped}\4 ${c_end}/g"
    return ${pipestatus[1]}
}
