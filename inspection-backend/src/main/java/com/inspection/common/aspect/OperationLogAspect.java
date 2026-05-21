package com.inspection.common.aspect;

import com.inspection.common.annotation.OperationLog;
import com.inspection.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

@Aspect
@Component
public class OperationLogAspect {
    private final OperationLogService operationLogService;
    private final HttpServletRequest request;

    public OperationLogAspect(OperationLogService operationLogService, HttpServletRequest request) {
        this.operationLogService = operationLogService;
        this.request = request;
    }

    @Around("@annotation(com.inspection.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        com.inspection.entity.OperationLog log = new com.inspection.entity.OperationLog();
        log.setModuleName(annotation.module());
        log.setOperationType(annotation.operationType());
        log.setTargetId(resolveTargetId(joinPoint.getArgs()));
        log.setDescription(buildDescription(annotation, joinPoint.getArgs()));
        log.setOperationTime(LocalDateTime.now());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            log.setOperator(authentication.getName());
            log.setOperatorRole(resolveRole(authentication.getAuthorities()));
        } else {
            log.setOperator("anonymous");
            log.setOperatorRole("ANONYMOUS");
        }

        operationLogService.save(log);
        return result;
    }

    private String resolveRole(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return "UNKNOWN";
        }
        String role = authorities.iterator().next().getAuthority();
        if (role != null && role.startsWith("ROLE_")) {
            return role.substring(5);
        }
        return role;
    }

    private Long resolveTargetId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Long id) {
                return id;
            }
            if (arg instanceof Integer id) {
                return id.longValue();
            }
            if (arg instanceof Map<?, ?> map) {
                Object idValue = map.get("id");
                if (idValue instanceof Number number) {
                    return number.longValue();
                }
            }
            Long reflected = resolveIdByReflection(arg);
            if (reflected != null) {
                return reflected;
            }
        }
        return null;
    }

    private Long resolveIdByReflection(Object arg) {
        if (arg == null) {
            return null;
        }
        try {
            Method getter = arg.getClass().getMethod("getId");
            Object value = getter.invoke(arg);
            if (value instanceof Number number) {
                return number.longValue();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String buildDescription(OperationLog annotation, Object[] args) {
        StringBuilder description = new StringBuilder();
        if (StringUtils.hasText(annotation.description())) {
            description.append(annotation.description());
        } else {
            description.append(annotation.operationType()).append("操作");
        }
        description.append("; URI=").append(request.getRequestURI());

        Integer affectedCount = resolveAffectedCount(args);
        if (affectedCount != null) {
            description.append("; 影响数量=").append(affectedCount);
        }

        return description.toString();
    }

    private Integer resolveAffectedCount(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Map<?, ?> map) {
                Object ids = map.get("ids");
                if (ids instanceof Collection<?> collection) {
                    return collection.size();
                }
            }
        }
        return null;
    }
}
