from inspect import Traceback
from readline import read_init_file
from subprocess import CalledProcessError
from pkg_resources import UnknownExtra
import yaml 
import sys
import semver
import traceback
import os
import requests
from utilities import load_yaml, read_cfg, log
from git_operations import BASE_BRANCH, git_obj
import configparser

cfg = read_cfg()
sonar_cfg = cfg["sonar"]

def check_chart_version(repo, pr_num):
    try:
        # skip check for imported helm repo
        topics = git_obj.git_repo_topics(repo)
        skip_topic = cfg["general"]["skip_version_check"]
        if skip_topic in topics:
            return True
        chart_file = cfg["github"]["chart_file"]
        IGNORE_FILES = cfg["github"]["IGNORE_FILES"]
        base_chart_content, _ = git_obj.read_file(repo, BASE_BRANCH, chart_file)
        previous_version = yaml.safe_load(base_chart_content).get('version')
        new_version = load_yaml(chart_file).get('version')
        changed_files = git_obj.changed_files(repo, int(pr_num))
        semver.Version.parse(new_version)
        for file in changed_files:
            if file in IGNORE_FILES:
                continue
            else:
                if semver.compare(new_version, previous_version) != 1:
                    log.error (f"new version {new_version} is not increased from branch {BASE_BRANCH} version {previous_version}")
                    sys.exit(1)
                else:
                    break
    except ValueError:
        log.error(f"Version {new_version} in chart.yaml is not valid semver")
        sys.exit(1)
    except Exception as e:
        log.debug("failure during checkinfo chart version")
        log.debug(traceback.print_exec())
        sys.exit(1)


def read_file(self, repo, branch, file):
    log.debug("Reading file {}".format(file))
    repo_obj = self.org.get_repo(repo)
    content_obj = repo_obj.get_contents(file, ref=branch)
    return content_obj.decoded_content.decode(), content_obj.sha



def modify_component_properties(repo, branch, revert=False, base_branch=BASE_BRANCH):
    """
    modifies the component.properties
    """
    if revert:
        replace_string = base_branch
    else:
        replace_string = branch
    regex = REGEX_COMPONENT_PROPERTIES
    file_name = FILE_COMPONENT_PROPERTIES
    log.info("changing {} for repo -> {} branch -> {}".format(file_name, repo, branch))
    git_file_content, file_sha = git_obj.read_file(repo, branch, file_name)
    log.info("Actual file content -\n{}".format(git_file_content))
    new_file_content = regex_replace(regex, replace_string, git_file_content)
    if new_file_content != git_file_content:
        git_obj.update_file(file_name, new_file_content, file_sha, repo, branch, COMMIT_MESSAGE)
    else:
        log.info("no changes made to -> {} as file alraedy updated in remote repo -> {} branch -> {}".format(file_name,repo,branch))
    







def read_file(self, repo, branch, file):
    log.debug("Reading file {}".format(file))
    repo_obj = self.org.get_repo(repo)
    try:
        content_obj = repo_obj.get_contents(file, ref=branch)
        return content_obj.decoded_content.decode(), True  # True indicates file exists
    except GithubException.UnknownObjectExcetion as e:
        log.warning(f"Failed to read file {file} from repo {repo} on branch {branch}. Error: {str(e)}")
        return None, False  # False indicates file does not exist
    

  
          