package ch.maxant.session_sizes.ui;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Scope;

import ch.maxant.session_sizes.services.DataService;

@Named
@Scope("request")
public class ViewInRequestScope {

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

	public String getSessionAttributes() {
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();
		return new SessionSizeHelper().getSessionAttributes(request);
	}

	public String getSessionSizeExcludingDuplicates()
			throws IllegalArgumentException, IllegalAccessException {
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();
		return new SessionSizeHelper()
				.getSessionSizeExcludingDuplicates(request);
	}
}
