package com.tgx.chess.pawn.login.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tgx.chess.pawn.login.model.Role;

@Repository("roleRepository")
public interface RoleRepository
        extends
        JpaRepository<Role, Integer>
{
    Role findByRole(String role);
}
