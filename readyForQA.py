import requests
import sys
import getopt
from decouple import config
from kubernetes import client

kube_config_path = "/home/prod-usa.jaspersystem.com/zhyderme/sanity3"

def check_kubernetes_deployment(jira_id, kube_config_path):
    # Load the Kubernetes configuration
    config.load_kube_config(kube_config_path)

    # Replace 'your_namespace' and 'your_deployment_name' with your actual namespace
    namespace = "cdr"
    deployment_name = "cdr-mediation-parser"

    # Initialize the Kubernetes API client

    apps_v1_api = client.AppsV1Api()

    try:
        # Fetch the deployment status
        deployment = apps_v1_api.read_namespaced_deployment_status(deployment_name, namespace)

        # Check if the number of ready replicas is equal to the desired replicas
        if deployment.status.ready_replicas == deployment.spec.replicas:
            print(f"Deployment for {jira_id} is successful.")
            return True
        else:
            print(f"Deployment for {jira_id} is not yet successful.")
            return False
    except client.exceptions.ApiException as e:
        print(f"Error occurred while checking the deployment for {jira_id}: {e}")
        return False

def update_jira_status(jira_id):

    read_url = f"https://zeeshanmedlari.atlassian.net/jira/rest/api/latest/issue/{jira_id}?fields=status"
    headers = {
        "Content_Type" : "application/json"
    }
    
    # Check current status of ticket
    response = requests.get(read_url, headers=headers, auth=(config("JIRA_USERNAME"), config("JIRA_PASSWORD")))

    if response.status_code == 200:
        status = response.json()

        if "fields" in status and "status" in status["fields"]:
            current_status = status["fields"]["status"]["name"]
        else:
            print("Unable to fetch the current status. Exiting.")
            sys.exit(1)

        if current_status != "Fixed":
            print(f"Status is not Fixed: Current Status is {current_status}. Exiting.")
            sys.exit(0)

        print("Bug is fixed - let's move it to ReadyForQA")

        update_url = f"https://zeeshanmedlari.atlassian.net/jira/rest/api/latest/issue/{jira_id}/transitions?expand=transitions.fields"
        update_data = {"transition": {"id": "711"}}

        # Move ticket to ReadyForQA
        update_response = requests.post(update_url, json=update_data, headers=headers, auth=(config("JIRA_USERNAME"), config("JIRA_PASSWORD")))

        if update_response.status_code == 204:
            print(f"Status of {jira_id} changed to ReadyForQA")
        else:
            print(f"ERROR: Status of {jira_id} could not be moved to ReadyForQA")
            sys.exit(1)

def main(argv):
    jira_id = ""

    try:
        opts, args = getopt.getopt(argv, "j:", ["jira_id="])
    except getopt.GetoptError:
        print("Usage: python script.py -j <jira_id>")
        sys.exit(1)

    for opt, arg in opts:
        if opt in ("-j", "--jira_id"):
            jira_id = arg

    if not jira_id:
        print("Please provide JIRA ID using -j option.")
        sys.exit(1)

    if not jira_id.startswith("CC-"):
        print("ALL JIRAs need to start with CC-")
        sys.exit(1)

    print(f"ID is {jira_id}")

    if jira_id in ["CC-0000", "CC-00000", "CC-000000"]:
        sys.exit()

    # Check the Kubernetes deployment status
    if not check_kubernetes_deployment(jira_id):
        sys.exit()

    # Update the JIRA status to "ReadyForQA"
    update_jira_status(jira_id)

if __name__ == "__main__":
    main(sys.argv[1:])