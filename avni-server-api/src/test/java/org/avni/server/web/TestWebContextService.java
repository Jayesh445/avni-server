package org.avni.server.web;

import org.avni.server.dao.OrganisationRepository;
import org.avni.server.dao.UserRepository;
import org.avni.server.domain.Organisation;
import org.avni.server.domain.User;
import org.avni.server.domain.UserContext;
import org.avni.server.framework.security.UserContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.avni.server.framework.security.AuthService.USER_AUTHORITY;
import static org.avni.server.framework.security.AuthenticationFilter.USER_NAME_HEADER;

@Component
public class TestWebContextService {
    @Autowired
    public TestRestTemplate template;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrganisationRepository organisationRepository;

    public void setUser(String name) {
        User user = userRepository.findByUsername(name);
        this.setUser(user);
    }

    public void setUser(User user) {
        setUserNameHeader(user.getUsername());
        UserContext userContext = new UserContext();
        userContext.setUser(user);
        UserContextHolder.create(userContext);

        if (user.getOrganisationId() != null) {
            Organisation organisation = organisationRepository.findOne(user.getOrganisationId());
            userContext.setOrganisation(organisation);
        }
        SimpleGrantedAuthority[] authorities = Stream.of(USER_AUTHORITY)
                .filter(authority -> userContext.getRoles().contains(authority.getAuthority()))
                .toArray(SimpleGrantedAuthority[]::new);

        setRoles(authorities);
    }

    private void setUserNameHeader(String userName) {
        template.getRestTemplate().setInterceptors(singletonList((request, body, execution) -> {
            request.getHeaders().add(USER_NAME_HEADER, userName);
            return execution.execute(request, body);
        }));
    }

    public void setRoles(SimpleGrantedAuthority... authorities) {
        if (authorities.length > 0) {
            String token = UUID.randomUUID().toString();
            AnonymousAuthenticationToken auth = new AnonymousAuthenticationToken(token, token, Arrays.asList(authorities));
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }
}
