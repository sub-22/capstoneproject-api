package com.jovinn.capstoneproject.service.impl;

import com.jovinn.capstoneproject.dto.PageResponse;
import com.jovinn.capstoneproject.dto.request.PostRequestRequest;
import com.jovinn.capstoneproject.dto.response.ApiResponse;
import com.jovinn.capstoneproject.dto.response.BoxResponse;
import com.jovinn.capstoneproject.dto.response.ListSellerApplyPostRequestResponse;
import com.jovinn.capstoneproject.dto.response.PostRequestResponse;
import com.jovinn.capstoneproject.enumerable.PostRequestStatus;
import com.jovinn.capstoneproject.exception.ApiException;
import com.jovinn.capstoneproject.exception.UnauthorizedException;
import com.jovinn.capstoneproject.model.*;
import com.jovinn.capstoneproject.repository.*;
import com.jovinn.capstoneproject.security.UserPrincipal;
import com.jovinn.capstoneproject.service.MilestoneContractService;
import com.jovinn.capstoneproject.service.NotificationService;
import com.jovinn.capstoneproject.service.PostRequestService;
import com.jovinn.capstoneproject.service.UserService;
import com.jovinn.capstoneproject.util.Pagination;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PostRequestServiceImpl implements PostRequestService {

    @Autowired
    private PostRequestRepository postRequestRepository;
    @Autowired
    private BuyerRepository buyerRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private SubCategoryRepository subCategoryRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private  SellerRepository sellerRepository;
    @Autowired
    private MilestoneContractService milestoneContractService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserService userService;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ApiResponse addPostRequest(PostRequestRequest request, UserPrincipal currentUser) {
        Buyer buyer = buyerRepository.findBuyerByUserId(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Buyer not found "));
        if (buyer.getUser().getId().equals(currentUser.getId()) &&
                buyer.getUser().getIsEnabled().equals(Boolean.TRUE)){
            PostRequest postRequest = new PostRequest();
            postRequest.setCategory(categoryRepository.findCategoryById(request.getCategoryId()));
            postRequest.setSubCategory(subCategoryRepository.findSubCategoryById(request.getSubCategoryId()));
            postRequest.setRecruitLevel(request.getRecruitLevel());
            postRequest.setSkills(skillRepository.findAllByNameIn(request.getSkillsName()));
            postRequest.setJobTitle(request.getJobTitle());
            postRequest.setShortRequirement(request.getShortRequirement());
            postRequest.setAttachFile(request.getAttachFile());
            postRequest.setStatus(PostRequestStatus.OPEN);
            postRequest.setContractCancelFee(request.getContractCancelFee());
            List<MilestoneContract> milestoneContractList = request.getMilestoneContracts();
            BigDecimal budget = new BigDecimal(0);
            for (MilestoneContract milestoneContract : milestoneContractList){
                budget = budget.add(milestoneContract.getMilestoneFee());
            }
            postRequest.setBudget(budget);
            postRequest.setUser(userRepository.findUserById(currentUser.getId()));
            Notification notification;
            List<User> usersGetInvite = request.getInvitedUsers();
            for (User userInvite: usersGetInvite){
                notification = new Notification();
                notification.setUser(userInvite);
                notification.setLink("/getPostRequestDetails/" + buyer.getUser().getId().toString() + "");
                notification.setShortContent("Bạn có lời mời làm việc mới từ " +
                        buyer.getUser().getFirstName() + " " + buyer.getUser().getLastName() +
                        " Kiểm tra ngay");
                notificationService.saveNotification(notification);
            }
            PostRequest savedPostRequest = postRequestRepository.save(postRequest);


            for (MilestoneContract milestoneContract : milestoneContractList){
                milestoneContract.setPostRequest(savedPostRequest);
                milestoneContractService.addMilestoneContract(milestoneContract);
            }
        }
        return new ApiResponse(Boolean.TRUE, "Khởi tạo yêu cầu thành công");
    }

    @Override
    public ApiResponse updatePostRequest(PostRequestRequest request, UUID id, UserPrincipal currentUser) {
        Buyer buyer = buyerRepository.findBuyerByUserId(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Buyer not found "));
        PostRequest post = postRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Post Request not found "));
        if (post.getUser().getId().equals(buyer.getUser().getId()) && post.getUser().getId().equals(currentUser.getId())){
            PostRequest savedPostRequest;
            if (request != null && post.getSellersApplyRequest().isEmpty() ){
                post.setCategory(categoryRepository.findCategoryById(request.getCategoryId()));
                post.setSubCategory(subCategoryRepository.findSubCategoryById(request.getSubCategoryId()));
                post.setRecruitLevel(request.getRecruitLevel());
                post.setSkills(skillRepository.findAllByNameIn(request.getSkillsName()));
                post.setJobTitle(request.getJobTitle());
                post.setShortRequirement(request.getShortRequirement());
                post.setAttachFile(request.getAttachFile());
                post.setStatus(PostRequestStatus.OPEN);
                post.setContractCancelFee(request.getContractCancelFee());
                List<MilestoneContract> milestoneContractList = request.getMilestoneContracts();
                BigDecimal budget = new BigDecimal(0) ;
                for (MilestoneContract milestoneContract : milestoneContractList){
                    budget = budget.add(milestoneContract.getMilestoneFee());
                }
                post.setBudget(budget);
                post.setUser(userRepository.findUserById(currentUser.getId()));
                savedPostRequest =  postRequestRepository.save(post);

                if (!milestoneContractList.isEmpty()){
                    for (MilestoneContract milestoneContract : milestoneContractList){
                        milestoneContract.setPostRequest(savedPostRequest);
                        milestoneContractService.addMilestoneContract(milestoneContract);
                    }
                }
                return new ApiResponse(Boolean.TRUE, "Cập nhật yêu cầu thành công");
            }
        }
        ApiResponse apiResponse = new ApiResponse(Boolean.FALSE, "You don't have permission");
        throw new UnauthorizedException(apiResponse);
    }

    @Override
    public Boolean deletePostRequest(UUID id) {
        postRequestRepository.deleteById(id);
        return true;
    }

    @Override
    public List<PostRequestResponse> getPostRequestByBuyerCreated(UserPrincipal currentUser) {
        Buyer buyer = buyerRepository.findBuyerByUserId(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Buyer not found "));
        List<PostRequest> postRequest;
       // PostRequestResponse postRequestResponse;
        List<PostRequestResponse> postRequestResponses = new ArrayList<>();
        if (buyer.getUser().getId().equals(currentUser.getId()) &&
                buyer.getUser().getIsEnabled().equals(Boolean.TRUE)) {
            postRequest = postRequestRepository.findAllByUser_Id(currentUser.getId());
            for (PostRequest postRequest1 : postRequest) {
                postRequestResponses.add(new PostRequestResponse(postRequest1.getId(), postRequest1.getCategory().getId(), postRequest1.getSubCategory().getId(),
                        postRequest1.getRecruitLevel(), postRequest1.getSkills(), postRequest1.getJobTitle(), postRequest1.getShortRequirement(),
                        postRequest1.getAttachFile(), postRequest1.getMilestoneContracts(), postRequest1.getContractCancelFee(), postRequest1.getBudget(),
                        userService.getListUserInvitedByPostRequestId(postRequest1.getId())));
            }
        }
        return postRequestResponses;
    }

    @Override
    public PageResponse<PostRequestResponse> getPostRequestByCategoryId(UUID categoryId, int page, int size, String sortBy, String sortDir) {
        List<PostRequestResponse> postRequestResponses = new ArrayList<>();
        Pageable pageable = Pagination.paginationCommon(page, size, sortBy, sortDir);
        Page<PostRequest> postRequests = postRequestRepository.findAllByCategory_Id(categoryId, pageable);
        List<PostRequest> list = postRequests.getContent();

        for (PostRequest postRequest:postRequests){
            postRequestResponses.add(new PostRequestResponse(postRequest.getId(), postRequest.getCategory().getId(),
                    postRequest.getSubCategory().getId(), postRequest.getJobTitle(),postRequest.getBudget(),
                    postRequest.getUser().getBuyer().getId(),postRequest.getUser().getFirstName(),postRequest.getUser().getLastName(),
                    postRequest.getUser().getCity(),postRequest.getCreateAt(), postRequest.getRecruitLevel(), postRequest.getSkills(),
                    postRequest.getShortRequirement(), postRequest.getMilestoneContracts(), postRequest.getContractCancelFee()));
        }

        List<PostRequestResponse> content = list.stream().map(
                        postRequest -> modelMapper.map(postRequest, PostRequestResponse.class))
                        .collect(Collectors.toList());
        return new PageResponse<>(content, postRequests.getNumber(), postRequests.getSize(), postRequests.getTotalElements(),
                postRequests.getTotalPages(), postRequests.isLast());
        //return postRequestResponses;
    }

    @Override
    public PostRequestResponse getPostRequestDetails(UUID postRequestId) {
        PostRequest postRequest = postRequestRepository.findPostRequestById(postRequestId);
        return new PostRequestResponse(postRequest.getCategory().getName(),postRequest.getSubCategory().getName(),postRequest.getRecruitLevel(),
                postRequest.getSkills(), postRequest.getJobTitle(), postRequest.getShortRequirement(), postRequest.getMilestoneContracts(),
                postRequest.getContractCancelFee(), postRequest.getBudget(),postRequest.getUser().getFirstName(),postRequest.getUser().getLastName(),
                postRequest.getUser().getCity(),postRequest.getUser().getCreateAt(),postRequestRepository.countPostRequestByUser_Id(postRequest.getUser().getId()));
//        return postRequestRepository.findPostRequestById(postRequestId);
    }

    @Override
    public ApiResponse sellerApplyRequest(UUID postRequestId, UserPrincipal currentUser) {
        Seller seller = sellerRepository.findSellerByUserId(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Seller not found "));
        PostRequest post = postRequestRepository.findById(postRequestId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Post Request not found "));
        List<Seller> sellersApply;
        if (seller.getUser().getId().equals(currentUser.getId()) &&
                seller.getUser().getIsEnabled().equals(Boolean.TRUE)){

                sellersApply = sellerRepository.findAllByPostRequests_Id(postRequestId);
                for (Seller sellerApply: sellersApply){
                    if (sellerApply.getId().equals(seller.getId())){
                        return new ApiResponse(Boolean.FALSE, "Bạn đã apply bài Post Request này");
                    }
                }
                sellersApply.add(seller);
                post.setSellersApplyRequest(sellersApply);
                postRequestRepository.save(post);

            return new ApiResponse(Boolean.TRUE,"Apply Post Request thành công");

        }

        ApiResponse apiResponse = new ApiResponse(Boolean.FALSE, "You don't have permission");
        throw new UnauthorizedException(apiResponse);
    }

    @Override
    public PageResponse<ListSellerApplyPostRequestResponse> getListSellerApply(UUID postRequestId, UserPrincipal currentUser,
                                                                               int page, int size, String sortDir) {
        PostRequest postRequest = postRequestRepository.findById(postRequestId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Không tìm thấy bài đăng"));
        Pageable pageable = Pagination.paginationCommon(page, size, "sellerId", sortDir);

//        List<Seller> sellers = postRequest.getSellersApplyRequest();
        if (postRequest.getUser().getId().equals(currentUser.getId())) {
            Page<Seller> sellers = sellerRepository.findAllByPostRequests_Id(postRequest.getId(), pageable);
            List<Seller> list = sellers.getContent();
            List<ListSellerApplyPostRequestResponse> content = list.stream().map(
                            sellerApplyPostRequest -> modelMapper.map(sellerApplyPostRequest, ListSellerApplyPostRequestResponse.class))
                            .collect(Collectors.toList());
            return new PageResponse<>(content, sellers.getNumber(), sellers.getSize(), sellers.getTotalElements(),
                    sellers.getTotalPages(), sellers.isLast());
            //return new ListSellerApplyPostRequestResponse(postRequest.getId(),postRequest.getSellersApplyRequest());
        }

        ApiResponse apiResponse = new ApiResponse(Boolean.FALSE, "You don't have permission");
        throw new UnauthorizedException(apiResponse);
    }

    @Override
    public List<PostRequestResponse> getAllPostRequest() {
        List<PostRequestResponse> postRequestResponses = new ArrayList<>();
        List<PostRequest> postRequests = postRequestRepository.findAll();
        for (PostRequest postRequest:postRequests){
            postRequestResponses.add(new PostRequestResponse(postRequest.getId(), postRequest.getCategory().getId(),
                    postRequest.getSubCategory().getId(), postRequest.getJobTitle(),postRequest.getBudget(),
                    postRequest.getUser().getBuyer().getId(),postRequest.getUser().getFirstName(),postRequest.getUser().getLastName(),
                    postRequest.getUser().getCity(),postRequest.getCreateAt(), postRequest.getRecruitLevel(), postRequest.getSkills(),
                    postRequest.getShortRequirement(), postRequest.getMilestoneContracts(), postRequest.getContractCancelFee()));
        }
        return postRequestResponses;
    }
}
