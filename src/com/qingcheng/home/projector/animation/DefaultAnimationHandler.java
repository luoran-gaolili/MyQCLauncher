
package com.qingcheng.home.projector.animation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Point;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import com.qingcheng.home.projector.FloatingActionMenu;

import java.util.ArrayList;
import java.util.List;


public class DefaultAnimationHandler extends MenuAnimationHandler {

    private static final String TAG = "Launcher.Anim";
    /** duration of animations, in milliseconds */
    protected static final int DURATION = 350;
    /** duration to wait between each of  */
    protected static final int LAG_BETWEEN_ITEMS = 20;
    /** holds the current state of animation */
    private boolean animating;

    private static final int inRadius = 260;
    public DefaultAnimationHandler() {
        setAnimating(false);
    }

    @Override
    public void animateMenuOpening(Point center) {
        super.animateMenuOpening(center);

        setAnimating(true);

        Animator lastAnimation = null;
        List<ObjectAnimator> animators = new ArrayList<>();
        for (int i = 0; i < menu.getSubActionItems().size(); i++) {

            menu.getSubActionItems().get(i).view.setAlpha(0);

            int targetY = menu.getSubActionItems().get(i).y - center.y + menu.getSubActionItems().get(i).height / 2;
            int targetX = menu.getSubActionItems().get(i).x - center.x + menu.getSubActionItems().get(i).width / 2;

            // TODO: 17-5-10
            int radius = 0;
            if(i == 0 || i == 4){
                radius = 561;
            }else if(i == 1 || i == 3){
                radius = 486;
            }else if(i == 2){
                radius = 474;
            }

            menu.getSubActionItems().get(i).view.setTranslationY(targetY * inRadius / radius);
            menu.getSubActionItems().get(i).view.setTranslationX(targetX * inRadius / radius);
            PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, targetX );
            PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, targetY );
            PropertyValuesHolder pvhA = PropertyValuesHolder.ofFloat(View.ALPHA, 1);

            final ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(menu.getSubActionItems().get(i).view, pvhX, pvhY, pvhA);
            animation.addListener(new SubActionItemAnimationListener(menu.getSubActionItems().get(i), ActionType.OPENING));

            if(i == 0) {
                lastAnimation = animation;
            }

            animators.add(animation);
        }
        if(lastAnimation != null) {
            lastAnimation.addListener(new LastAnimationListener());
        }

        smallWeatherView.setAlpha(0);
        smallWeatherView.setTranslationY(240);
        PropertyValuesHolder smallWeatherViewY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0);
        PropertyValuesHolder smallWeatherViewA = PropertyValuesHolder.ofFloat(View.ALPHA, 1);

        final ObjectAnimator smallWeatherViewAnimation = ObjectAnimator.ofPropertyValuesHolder(smallWeatherView, smallWeatherViewY, smallWeatherViewA);

        weatherView.setAlpha(1);
        weatherView.setTranslationY(0);
        PropertyValuesHolder weatherViewY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -480);
        PropertyValuesHolder weatherViewA = PropertyValuesHolder.ofFloat(View.ALPHA, 0);

        final ObjectAnimator weatherViewAnimation = ObjectAnimator.ofPropertyValuesHolder(weatherView, weatherViewY, weatherViewA);

//        img_outline_1.setAlpha(1);
//        PropertyValuesHolder img_outline_1A = PropertyValuesHolder.ofFloat(View.ALPHA, 0);
//        final ObjectAnimator img_outline_1Animation = ObjectAnimator.ofPropertyValuesHolder(img_outline_1, img_outline_1A);

//        img_outline_2.setAlpha(1);
//        PropertyValuesHolder img_outline_2A = PropertyValuesHolder.ofFloat(View.ALPHA, 0);
//        final ObjectAnimator img_outline_2Animation = ObjectAnimator.ofPropertyValuesHolder(img_outline_2, img_outline_2A);

//        img_outline_3.setAlpha(1);
//        PropertyValuesHolder img_outline_3A = PropertyValuesHolder.ofFloat(View.ALPHA, 0);
//        final ObjectAnimator img_outline_3Animation = ObjectAnimator.ofPropertyValuesHolder(img_outline_3, img_outline_3A);

        projector_logo.setAlpha(1);
        PropertyValuesHolder projector_logoA = PropertyValuesHolder.ofFloat(View.ALPHA, 0);
        final ObjectAnimator projector_logoAnimation = ObjectAnimator.ofPropertyValuesHolder(projector_logo, projector_logoA);

//        img_inline_1.setAlpha(0);
//        PropertyValuesHolder img_inline_1A = PropertyValuesHolder.ofFloat(View.ALPHA, 1);
//        final ObjectAnimator img_inline_1Animation = ObjectAnimator.ofPropertyValuesHolder(img_inline_1, img_inline_1A);

        projector_standby.setAlpha(0);
        PropertyValuesHolder projector_standbyA = PropertyValuesHolder.ofFloat(View.ALPHA, 1);
        final ObjectAnimator projector_standbyAnimation = ObjectAnimator.ofPropertyValuesHolder(projector_standby, projector_standbyA);

//        img_inline_2.setAlpha(1);
//        PropertyValuesHolder img_inline_2A = PropertyValuesHolder.ofFloat(View.ALPHA, 0);
//        final ObjectAnimator img_inline_2Animation = ObjectAnimator.ofPropertyValuesHolder(img_inline_2, img_inline_2A);

        AnimatorSet animSet = new AnimatorSet();
        animSet.setDuration(DURATION);
        animSet.setInterpolator(new AccelerateDecelerateInterpolator());

        animSet.playTogether(animators.get(0), animators.get(1), animators.get(2), animators.get(3),
                animators.get(4), smallWeatherViewAnimation, weatherViewAnimation,
//                img_outline_1Animation, img_outline_2Animation, img_outline_3Animation,
                /*img_inline_1Animation, img_inline_2Animation,*/ projector_standbyAnimation,
                projector_logoAnimation);
        animSet.start();

    }

    @Override
    public void animateMenuClosing(Point center) {
        super.animateMenuOpening(center);

        setAnimating(true);

        Animator lastAnimation = null;
        List<ObjectAnimator> animators = new ArrayList<>();
        for (int i = 0; i < menu.getSubActionItems().size(); i++) {
            int targetX = - (menu.getSubActionItems().get(i).x - center.x + menu.getSubActionItems().get(i).width / 2);
            int targetY = - (menu.getSubActionItems().get(i).y - center.y + menu.getSubActionItems().get(i).height / 2);

            // TODO: 17-5-10
            int radius = 0;
            if(i == 0 || i == 4){
                radius = 561;
            }else if(i == 1 || i == 3){
                radius = 486;
            }else if(i == 2){
                radius = 474;
            }

            PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, targetX * inRadius / radius);
            PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, targetY * inRadius / radius);
            PropertyValuesHolder pvhA = PropertyValuesHolder.ofFloat(View.ALPHA, 0);

            final ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(menu.getSubActionItems().get(i).view, pvhX, pvhY, pvhA);
            animation.addListener(new SubActionItemAnimationListener(menu.getSubActionItems().get(i), ActionType.CLOSING));

            if(i == 0) {
                lastAnimation = animation;
            }

            animators.add(animation);
        }

        if(menu.getSubMenuButton() != null && menu.getSubMenuButton().isMenuOpened()){
            for (int i = 0; i < menu.getSubMenuButton().getViewItems().size(); i++) {
                int targetX = - (menu.getSubActionItems().get(i).x - center.x + menu.getSubActionItems().get(i).width / 2);
                int targetY = - (menu.getSubActionItems().get(i).y - center.y + menu.getSubActionItems().get(i).height / 2);

                // TODO: 17-5-10
                int radius = 0;
                if(i == 0 || i == 4){
//                    radius = 561;
                    radius = 486;
                }else if(i == 1 || i == 3){
//                    radius = 486;
                    radius = 561;
                }else if(i == 2){
                    radius = 486;
//                    radius = 474;
                }

                PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, targetX * inRadius / radius);
                PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, targetY * inRadius / radius);
                PropertyValuesHolder pvhA = PropertyValuesHolder.ofFloat(View.ALPHA, 0);

                final ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(menu.getSubMenuButton().getViewItems().get(i), pvhX, pvhY, pvhA);
//                animation.addListener(new SubActionItemAnimationListener(menu.getSubActionItems().get(i), ActionType.CLOSING));

//                if(i == 0) {
//                    lastAnimation = animation;
//                }

                animators.add(animation);
            }
        }
        if(menu.getSubMenuButtonG() != null && menu.getSubMenuButtonG().isMenuOpened()){
            for (int i = 0; i < menu.getSubMenuButtonG().getViewItems().size(); i++) {
                int targetX = - (menu.getSubActionItems().get(i).x - center.x + menu.getSubActionItems().get(i).width / 2);
                int targetY = - (menu.getSubActionItems().get(i).y - center.y + menu.getSubActionItems().get(i).height / 2);

                // TODO: 17-5-10
                int radius = 0;
                if(i == 0 || i == 4){
//                    radius = 561;
                    radius = 486;
                }else if(i == 1 || i == 3){
//                    radius = 486;
                    radius = 561;
                }else if(i == 2){
                    radius = 486;
//                    radius = 474;
                }

                PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, targetX * inRadius / radius);
                PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, targetY * inRadius / radius);
                PropertyValuesHolder pvhA = PropertyValuesHolder.ofFloat(View.ALPHA, 0);

                final ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(menu.getSubMenuButtonG().getViewItems().get(i), pvhX, pvhY, pvhA);
//                animation.addListener(new SubActionItemAnimationListener(menu.getSubActionItems().get(i), ActionType.CLOSING));

//                if(i == 0) {
//                    lastAnimation = animation;
//                }

                animators.add(animation);
            }
        }
        if(menu.getSubMenuButtonV() != null && menu.getSubMenuButtonV().isMenuOpened()){
            for (int i = 0; i < menu.getSubMenuButtonV().getViewItems().size(); i++) {
                int targetX = - (menu.getSubActionItems().get(i).x - center.x + menu.getSubActionItems().get(i).width / 2);
                int targetY = - (menu.getSubActionItems().get(i).y - center.y + menu.getSubActionItems().get(i).height / 2);

                // TODO: 17-5-10
                int radius = 0;
                if(i == 0 || i == 4){
//                    radius = 561;
                    radius = 486;
                }else if(i == 1 || i == 3){
//                    radius = 486;
                    radius = 561;
                }else if(i == 2){
                    radius = 486;
//                    radius = 474;
                }

                PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, targetX * inRadius / radius);
                PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, targetY * inRadius / radius);
                PropertyValuesHolder pvhA = PropertyValuesHolder.ofFloat(View.ALPHA, 0);

                final ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(menu.getSubMenuButtonV().getViewItems().get(i), pvhX, pvhY, pvhA);
                animators.add(animation);
            }
        }

        if(lastAnimation != null) {
            lastAnimation.addListener(new LastAnimationListener());
        }

        smallWeatherView.setAlpha(1);
        smallWeatherView.setTranslationY(0);
        PropertyValuesHolder smallWeatherViewY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 240);
        PropertyValuesHolder smallWeatherViewA = PropertyValuesHolder.ofFloat(View.ALPHA, 0);

        final ObjectAnimator smallWeatherViewAnimation = ObjectAnimator.ofPropertyValuesHolder(smallWeatherView, smallWeatherViewY, smallWeatherViewA);

        weatherView.setAlpha(0);
        weatherView.setTranslationY(-480);
        PropertyValuesHolder weatherViewY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0);
        PropertyValuesHolder weatherViewA = PropertyValuesHolder.ofFloat(View.ALPHA, 1);

        final ObjectAnimator weatherViewAnimation = ObjectAnimator.ofPropertyValuesHolder(weatherView, weatherViewY, weatherViewA);

//        img_outline_1.setAlpha(0);
//        PropertyValuesHolder img_outline_1A = PropertyValuesHolder.ofFloat(View.ALPHA, 1);
//        final ObjectAnimator img_outline_1Animation = ObjectAnimator.ofPropertyValuesHolder(img_outline_1, img_outline_1A);
//
//        img_outline_2.setAlpha(0);
//        PropertyValuesHolder img_outline_2A = PropertyValuesHolder.ofFloat(View.ALPHA, 1);
//        final ObjectAnimator img_outline_2Animation = ObjectAnimator.ofPropertyValuesHolder(img_outline_2, img_outline_2A);
//
//        img_outline_3.setAlpha(0);
//        PropertyValuesHolder img_outline_3A = PropertyValuesHolder.ofFloat(View.ALPHA, 1);
//        final ObjectAnimator img_outline_3Animation = ObjectAnimator.ofPropertyValuesHolder(img_outline_3, img_outline_3A);

//        img_inline_1.setAlpha(1);
//        PropertyValuesHolder img_inline_1A = PropertyValuesHolder.ofFloat(View.ALPHA, 0);
//        final ObjectAnimator img_inline_1Animation = ObjectAnimator.ofPropertyValuesHolder(img_inline_1, img_inline_1A);
//
//        img_inline_2.setAlpha(0);
//        PropertyValuesHolder img_inline_2A = PropertyValuesHolder.ofFloat(View.ALPHA, 1);
//        final ObjectAnimator img_inline_2Animation = ObjectAnimator.ofPropertyValuesHolder(img_inline_2, img_inline_2A);

        projector_standby.setAlpha(1);
        PropertyValuesHolder projector_standbyA = PropertyValuesHolder.ofFloat(View.ALPHA, 0);
        final ObjectAnimator projector_standbyAnimation = ObjectAnimator.ofPropertyValuesHolder(projector_standby, projector_standbyA);

        projector_logo.setAlpha(0);
        PropertyValuesHolder projector_logoA = PropertyValuesHolder.ofFloat(View.ALPHA, 1);
        final ObjectAnimator projector_logoAnimation = ObjectAnimator.ofPropertyValuesHolder(projector_logo, projector_logoA);

        AnimatorSet animSet = new AnimatorSet();
        animSet.setDuration(250);
        animSet.setInterpolator(new AccelerateInterpolator());

        animSet.playTogether(animators.get(0), animators.get(1), animators.get(2), animators.get(3),
                animators.get(4), smallWeatherViewAnimation, weatherViewAnimation,
//                img_outline_1Animation, img_outline_2Animation, img_outline_3Animation,
                /*img_inline_1Animation, img_inline_2Animation,*/ projector_standbyAnimation,
                projector_logoAnimation);
        animSet.start();
    }

    @Override
    public boolean isAnimating() {
        return animating;
    }

    @Override
    protected void setAnimating(boolean animating) {
        this.animating = animating;
    }

    protected class SubActionItemAnimationListener implements Animator.AnimatorListener {

        private FloatingActionMenu.Item subActionItem;
        private ActionType actionType;

        public SubActionItemAnimationListener(FloatingActionMenu.Item subActionItem, ActionType actionType) {
            this.subActionItem = subActionItem;
            this.actionType = actionType;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            Log.d(TAG, "onAnimationStart: ");
            subActionItem.view.setClickable(false);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            restoreSubActionViewAfterAnimation(subActionItem, actionType);
            subActionItem.view.setClickable(true);
            Log.d(TAG, "onAnimationEnd: ");
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            restoreSubActionViewAfterAnimation(subActionItem, actionType);
            subActionItem.view.setClickable(true);
            Log.d(TAG, "onAnimationCancel: ");
        }

        @Override public void onAnimationRepeat(Animator animation) {}
    }
}
