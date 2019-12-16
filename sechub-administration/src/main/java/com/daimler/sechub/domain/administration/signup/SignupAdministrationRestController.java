// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.administration.signup;

import java.util.List;

import javax.annotation.security.RolesAllowed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.daimler.sechub.domain.administration.AdministrationAPIConstants;
import com.daimler.sechub.sharedkernel.Profiles;
import com.daimler.sechub.sharedkernel.RoleConstants;
import com.daimler.sechub.sharedkernel.Step;
import com.daimler.sechub.sharedkernel.usecases.admin.signup.UseCaseAdministratorDeletesSignup;
import com.daimler.sechub.sharedkernel.usecases.admin.signup.UseCaseAdministratorListsOpenUserSignups;



/**
 * Self registration rest controller - restricted access to super admins
 * 
 * @author Albert Tregnaghi
 *
 */
@RestController
@EnableAutoConfiguration
@RolesAllowed(RoleConstants.ROLE_SUPERADMIN)
@Profile(Profiles.ADMIN_ACCESS)
public class SignupAdministrationRestController {
	@Autowired
	private SignupRepository repository;
	
	@Autowired
	private SignupDeleteService deleteService;

	/* @formatter:off */
	@UseCaseAdministratorDeletesSignup(@Step(number=1, name="Rest API call",description="Rest api called to remove user signup",needsRestDoc=true))
	@RequestMapping(path = AdministrationAPIConstants.API_DELETE_SIGNUP, method = RequestMethod.DELETE, produces= {MediaType.APPLICATION_JSON_VALUE})
	public void deleteSignup(@PathVariable(name="userId") String userId) {
		deleteService.delete(userId);
		/* @formatter:on */
	}
	
	/* @formatter:off */
	@UseCaseAdministratorListsOpenUserSignups(@Step(number=1,name="Rest call",description="All self registrations are returned as json",needsRestDoc=true))
	@RequestMapping(path = AdministrationAPIConstants.API_LIST_USER_SIGNUPS, method = RequestMethod.GET, produces= {MediaType.APPLICATION_JSON_VALUE})
	public List<Signup> listUserSignups() {
		/* @formatter:on */
		return repository.findAll();
	}

}
