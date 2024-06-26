package com.longd.longd.user.service;

import com.longd.longd.coupleList.db.entity.CoupleList;
import com.longd.longd.coupleList.db.repository.CoupleListRepository;
import com.longd.longd.user.db.dto.CustomOAuth2User;
import com.longd.longd.user.db.entity.NationList;
import com.longd.longd.user.db.entity.User;
import com.longd.longd.user.db.repository.NationListRepository;
import com.longd.longd.user.db.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class UserServiceImpl implements UserService{

    @Autowired
    UserRepository userRepository;

    @Autowired
    CoupleListRepository coupleListRepository;

    @Autowired
    NationListRepository nationListRepository;

    public void userRegist(User user) {
        log.info(user.toString());
        String[] tmp = user.getBirth().split("-");
        if ( user.getPasswordSimple() == null ) {
            //혹시 0228이면 어떻게 될까 ?
            //생일 값은 반드시 있다 가정
            user.setPasswordSimple(Integer.parseInt(tmp[1] + tmp[2]));
        }
        user.setBirthYear(tmp[0]);
        user.setBirthMonth(tmp[1]);
        user.setBirthDay(tmp[2]);
        if ( user.getProfilePicture() == null || user.getProfilePicture().isEmpty() ) {
            user.setProfilePicture("https://longdssafy.s3.ap-northeast-2.amazonaws.com/52efc0a1-f3fa-4e70-a803-748fd41bca7cmainIMG.png");
        }

        userRepository.save(user);
    }

    public boolean resetSimplePassword() {
        User user = userState().get();
        if( user == null) {
            return false;
        } else {
            String[] tmp = user.getBirth().split("-");
            user.setPasswordSimple(Integer.parseInt(tmp[1] + tmp[2]));
            userRepository.save(user);
            return true;
        }

    }

    public String userDelete() {
        User user = userState().get();
        int coupleId = user.getCoupleListId();
        CoupleList coupleList = coupleListRepository.findById(coupleId).get();

        if(coupleList.getUserFirst() == user.getId()) {
            coupleList.setUserFirst(null);
            coupleListRepository.save(coupleList);
            userRepository.delete(user);
            return "회원 탈퇴 성공";
        } else if (coupleList.getUserSecond() == user.getId()) {
            coupleList.setUserSecond(null);
            coupleListRepository.save(coupleList);
            userRepository.delete(user);
            return "회원 탈퇴 성공";
        } else {
            log.error("있을 수 없는 상황");
            return "있을 수 없는 상황";
        }
    }

    @Override
    public String userDisconnect() {
        User user = userState().get();
        int coupleId = user.getCoupleListId();
        CoupleList coupleList = coupleListRepository.findById(coupleId).get();

        if(coupleList.getUserFirst() == user.getId()) {
            coupleList.setUserFirst(null);
            coupleListRepository.save(coupleList);
            user.setCoupleListId(null);
            userRepository.save(user);
            return "연결 끊기 성공";
        } else if (coupleList.getUserSecond() == user.getId()) {
            coupleList.setUserSecond(null);
            coupleListRepository.save(coupleList);
            user.setCoupleListId(null);
            userRepository.save(user);
            return "연결 끊기 성공";
        } else {
            log.error("있을 수 없는 상황");
            return "있을 수 없는 상황";
        }
    }


    public Optional<User> userState() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        log.info(authentication.toString());
        if(authentication.getPrincipal().toString().equals("anonymousUser")) {
            System.out.println("유저 상태 로그인 되어있지 않음");
            //임시 사용자 반환(제거)
//            return userRepository.findById(28);
            return null;
        }
        CustomOAuth2User info = (CustomOAuth2User) authentication.getPrincipal();
        Optional<User> user = null;
        if(info.getProvider().equals("kakao")) {
            //카카오
            user = userRepository.findByUserId(info.getAttributes().get("id").toString());
        } else if (info.getProvider().equals("google")) {
            //구글
            user = userRepository.findByUserId(info.getAttributes().get("sub").toString());
        } else if (info.getProvider().equals("naver")) {
            //네이버
            user = userRepository.findByUserId(info.getAttributes().get("id").toString());
        } else {
            System.out.println("누구세요 ?");
        }

        return user;

    }

    public User BaseInfo() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        System.out.println(authentication.toString());
        CustomOAuth2User info = (CustomOAuth2User) authentication.getPrincipal();

        System.out.println(info.getAuthorities().stream().toArray().toString());

        User user = new User();

        user.setUserId(info.getProviderId());
        user.setName(info.getName());
        user.setProvider(info.getProvider());

        return user;
    }

    public List<NationList> getNationList() {
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        return nationListRepository.findAll(sort);
    }

    public String WeblockCheck(String simplePassword) {
        User user = userState().get();
        int tmp = -1;

        try {
            tmp = Integer.parseInt(simplePassword);
        } catch (NumberFormatException e) {
            return "정수가 아닌값이 감지되었습니다.";
        }

        if (user.getPasswordSimple() == tmp ) {
            return "비밀번호 인증 성공";
        } else {
            return "비밀번호가 다릅니다.";
        }
    }
}
