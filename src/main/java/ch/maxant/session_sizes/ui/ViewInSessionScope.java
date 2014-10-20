package ch.maxant.session_sizes.ui;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Scope;

import ch.maxant.session_sizes.services.DataService;

@Named
@Scope("session")
public class ViewInSessionScope implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * since tomcat and other ASs like to serialize session scoped attributes,
	 * and DataService shouldn't be serializable because its a stateless service
	 * we would like to make the following field transient. but doing so causes
	 * null pointer exceptions when objects of this class are deserialized! as
	 * such, its pointless declaring this class to be serializable. it is
	 * serializable, but simply doesnt work after deserialization.
	 */
	@Inject
	private/* transient doesnt really work */DataService dataService;

	public String getSizeOfData() {

		// use the service, but dont hold on to the data it returns.
		dataService.generateData();

		// now measure size of session
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();
		return new SessionSizeHelper().getSessionSize(request);
	}
}
