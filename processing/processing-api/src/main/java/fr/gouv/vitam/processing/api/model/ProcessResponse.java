/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.processing.api.model;

import java.util.List;
import java.util.Map;

/**
 * 
 * Process Response class
 * 
 * contains global process status, messages and list of action results
 */
public class ProcessResponse implements Response {
	/**
	 * Enum status code
	 */
	private StatusCode status;

	/**
	 * List of functional messages
	 */
	private List<String> messages;
	/**
	 * List of steps 's responses
	 * 
	 * 
	 * key is stepName
	 * 
	 * object is list of response 's action
	 */
	private Map<String, List<Response>> stepResponses;

	@Override
	public StatusCode getStatus() {
		return status;
	}

	@Override
	public void setStatus(StatusCode status) {
		this.status = status;
	}

	@Override
	public List<String> getMessages() {
		return messages;
	}

	@Override
	public void setMessages(List<String> messages) {
		this.messages = messages;
	}

	/**
	 * @return the stepResponses
	 */
	public Map<String, List<Response>> getStepResponses() {
		return stepResponses;
	}

	/**
	 * @param stepResponses
	 *            the stepResponses to set
	 */
	public void setStepResponses(Map<String, List<Response>> stepResponses) {
		if (stepResponses != null && !stepResponses.isEmpty()) {
			stepResponses.forEach((actionKey, responses) -> this.status = getGlobalProcessStatusCode(responses));
		}
		this.stepResponses = stepResponses;
	}

	private StatusCode getGlobalProcessStatusCode(List<Response> responses) {
		StatusCode statusCode = StatusCode.OK;

		if (responses != null) {
			for (Response response : responses) {
				if (StatusCode.FATAL == response.getStatus()) {
					statusCode = StatusCode.FATAL;
					break;
				} else if (StatusCode.KO == response.getStatus()) {
					statusCode = StatusCode.KO;
					continue;
				} else if (StatusCode.WARNING == response.getStatus() && this.status != StatusCode.KO) {
					statusCode = StatusCode.WARNING;
				}
			}
		}
		return statusCode;
	}
}
