import requests
import sys

def update_jira_status(jira_id):
    jira_credentials = "zeeshan:'Anything'"

    read_cmd = f"https://zeeshanmedlari.atlassian.net/jira/rest/api/latest/issue/{jira_id}?fields=status"
    
    # Check current status of ticket
    response = requests.get(read_cmd, auth=jira_credentials)
    if response.status_code == 200:
        status = response.json()
    else:
        print(f"Failed to fetch the current status for {jira_id}. Exiting.")
        sys.exit(1)

    if "fields" in status and "status" in status["fields"]:
        current_status = status["fields"]["status"]["name"]
    else:
        print("Unable to fetch the current status. Exiting.")
        sys.exit(1)

    if current_status != "Fixed":
        print(f"Status is not Fixed: Current Status is {current_status}. Exiting.")
        sys.exit(0)

    print("Bug is fixed - let's move it to ReadyForQA")

    update_cmd = f"https://zeeshanmedlari.atlassian.net/jira/rest/api/latest/issue/{jira_id}/transitions?expand=transitions.fields"
    data = {"transition": {"id": "711"}}

    # Move ticket to ReadyForQA
    response = requests.post(update_cmd, auth=jira_credentials, json=data)
    if response.status_code == 204:
        print(f"Status of {jira_id} changed to ReadyForQA")
    else:
        print(f"ERROR: Status of {jira_id} could not be moved to ReadyForQA. Exiting.")
        sys.exit(1)







def update_jira_status(jira_id):
    read_cmd = f"curl --tlsv1.2 --insecure -u {jira_credentials} -X GET -H \"Content-Type: application/json\" https://zeeshanmedlari.atlassian.net/jira/rest/api/latest/issue/{jira_id}?fields=status"
    
    # Check current status of ticket
    status = requests.get(read_cmd).json()

    if "fields" in status and "status" in status["fields"]:
        current_status = status["fields"]["status"]["name"]
    else:
        print("Unable to fetch the current status. Exiting.")
        sys.exit(1)

    if current_status != "Fixed":
        print(f"Status is not Fixed: Current Status is {current_status}. Exiting.")
        sys.exit(0)

    print("Bug is fixed - let's move it to ReadyForQA")

    update_cmd = f"curl --tlsv1.2 --insecure -u {jira_credentials} -X POST --data '{{\"transition\":{{\"id\":\"711\"}}}}' -H \"Content-Type: application/json\" https://zeeshanmedlari.atlassian.net/jira/rest/api/latest/issue/{jira_id}/transitions?expand=transitions.fields"

    # Move ticket to ReadyForQA
    response = requests.post(update_cmd)
    if response.status_code == 204:
        print(f"Status of {jira_id} changed to ReadyForQA")
    else:
        print(f"ERROR: Status of {jira_id} could not be moved to ReadyForQA. Exiting.")
        sys.exit(1)

def main(argv):
    jira_id =