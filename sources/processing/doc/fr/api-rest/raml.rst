API Rest
########

    POST /operations: permet de soumettre un opéraiton (workflow et container) à l'exécution
    
    Request:
		Headers: 
			X-RequestId: (string)
		Body
			Type: application/json
			Example:
			
{
	"id": "id1",
	"context": {
		"source": "id2",
		"contract": "id3",
		"service": "id4",
		"priority": 100
	},
	"steps": [{
		"pool": "idpool1",
		"actions": [{
			"action": "idaction_1.1",
			"arguments": {
				"nom_champ_1.1.1": "jxpath_1.1.1",
				"nom_champ_1.1.2": "jxpath_1.1.2"
			},
			"results": {
				"nom_champ_1.1.3": null,
				"nom_champ_1.1.4": null
			},
			"status" : "Running",
			"post_assign": [{
				"target": "jxpath_1.1.3",
				"result": "construction_1.1.1"
			}, {
				"target": "jxpath_1.1.4",
				"result": "construction_1.1.2"
			}],
			"start_time" : "1990-12-31T23:59:60Z",
			"end_time" : "1990-12-31T23:59:60Z"
		}, {
			"action": "idaction_1.2",
			"arguments": {
				"nom_champ_1.2.1": "jxpath_1.2.1",
				"nom_champ_1.2.2": "jxpath_1.2.2"
			},
			"results": {
				"nom_champ_1.2.3": null,
				"nom_champ_1.2.4": null
			},
			"status" : "Running",
			"post_assign": [{
				"target": "jxpath_1.2.3",
				"result": "construction_1.2.1"
			}, {
				"target": "jxpath_1.2.4",
				"result": "construction_1.2.2"
			}],
			"start_time" : "1990-12-31T23:59:60Z",
			"end_time" : "1990-12-31T23:59:60Z"
		}],
		"results": {
			"nom_champ_1.3": null,
			"nom_champ_1.4": null
		},
		"post_assign": [{
			"target": "jxpath_1.3.1",
			"result": "construction_1.3.1"
		}, {
			"target": "jxpath_1.3.2",
			"result": "construction_1.3.2"
		}],
		"start_time" : "1990-12-31T23:59:60Z",
		"end_time" : "1990-12-31T23:59:60Z"
	}, {
		"pool": "idpool1",
		"actions": [{
			"action": "idaction_2.1",
			"arguments": {
				"nom_champ_2.1.1": "jxpath_2.1.1",
				"nom_champ_2.1.2": "jxpath_2.1.2"
			},
			"results": {
				"nom_champ_2.1.3": null,
				"nom_champ_2.1.4": null
			},
			"status" : "NotStarted",
			"post_assign": [{
				"target": "jxpath_2.1.3",
				"result": "construction_2.1.1"
			}, {
				"target": "jxpath_2.1.4",
				"result": "construction_2.1.2"
			}],
			"start_time" : "1990-12-31T23:59:60Z",
			"end_time" : "1990-12-31T23:59:60Z"
		}, {
			"action": "idaction_2.2",
			"arguments": {
				"nom_champ_2.2.1": "jxpath_2.2.1",
				"nom_champ_2.2.2": "jxpath_2.2.2"
			},
			"results": {
				"nom_champ_2.2.3": null,
				"nom_champ_2.2.4": null
			},
			"status" : "NotStarted",
			"post_assign": [{
				"target": "jxpath_2.2.3",
				"result": "construction_2.2.1"
			}, {
				"target": "jxpath_2.2.4",
				"result": "construction_2.2.2"
			}],
			"start_time" : "1990-12-31T23:59:60Z",
			"end_time" : "1990-12-31T23:59:60Z"
		}],
		"results": {
			"nom_champ_2.3": null,
			"nom_champ_2.4": null
		},
		"post_assign": [{
			"target": "jxpath_2.3.1",
			"result": "construction_2.3.1"
		}, {
			"target": "jxpath_2.3.2",
			"result": "construction_2.3.2"
		}],
		"start_time" : "1990-12-31T23:59:60Z",
		"end_time" : "1990-12-31T23:59:60Z"
	}],
	"results": {
		"nom_champ_3": null,
		"nom_champ_4": null
	},
	"status" : "Running",
	"post_assign": [{
		"target": "jxpath_3",
		"result": "construction_3"
	}, {
		"target": "jxpath_4",
		"result": "construction_5"
	}],
	"creation_time" : "1990-12-31T23:59:60Z",
	"start_time" : "1990-12-31T23:59:60Z",
	"end_time" : "1990-12-31T23:59:60Z"
}
		
		
	Response:
		HTTP status code 201: Crée
		HTTP status code 401: Non autorisée
		HTTP status code 404: Introuvable
		HTTP status code 412: Échec de précondition
		   