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

	@Inject
	private DataService dataService;

	public String getSizeOfData() {

		// use the service, but dont hold on to the data it returns.
		dataService.generateData();

		// now measure size of session
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();
		return new SessionSizeHelper().getSessionSize(request);
	}
}
