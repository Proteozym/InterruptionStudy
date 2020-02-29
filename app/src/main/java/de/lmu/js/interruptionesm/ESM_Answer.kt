package de.lmu.js.interruptionesm

class ESM_Answer {

    data class ESM_Answer_Data(

        var esm_type: Int,
        var esm_title: String,
        var esm_instructions: String,
        var esm_submit: String,
        var esm_answer: String
        //{"esm_type":1,"esm_title":"What is on your mind?","esm_submit":"Next","esm_instructions":"Tell us how you feel"}
        //{"esm_type":2,"esm_radios":["Bored","Fantastic"],"esm_title":"Are you...","esm_instructions":"Pick one!","esm_submit":"OK"}
        //{"esm_type":3,"esm_checkboxes":["Option 1","Option 2","Other"],"esm_title":"Checkbox","esm_submit":"OK","esm_instructions":"Multiple choice is allowed"}
        //{"esm_type":4,"esm_likert_max":5,"esm_likert_max_label":"Great","esm_likert_min_label":"Poor","esm_likert_step":1,"esm_title":"Likert","esm_instructions":"Likert ESM","":"OK"}
        //{"esm_type":5,"esm_quick_answers":["Yes","No"],"esm_instructions":"Quick Answers ESM"}
        //{"esm_type":6,"esm_scale_max":100,"esm_scale_min":0,"esm_scale_start":50,"esm_scale_max_label":"Perfect","esm_scale_min_label":"Poor","esm_scale_step":10,"esm_title":"Scale","esm_instructions":"Scale ESM","esm_submit":"OK"}
        //{"esm_type":9,"esm_title":"Numeric","esm_instructions":"The user can enter a number","esm_submit":"OK"}
        //{"esm_type":10,"esm_title":"ESM Web","esm_instructions":"Please fill out this online survey. Press OK when done.","esm_submit":"OK","esm_url":"https://www.google.com"}


    )

    data class ESM_Type_Radio (
        var esm_radios :Array<String> = arrayOf<String>()

    )

    data class ESM_Type_Checkboxes (
        var esm_checkboxes: Array<String> = arrayOf<String>()
    )

    data class ESM_Type_Likert (
        var esm_likert_max: Int,
        var esm_likert_max_label: String,
        var esm_likert_min_label: String,
        var esm_likert_step: Int

    )
    data class ESM_Type_QuickAnswers (
        var esm_quick_answers: Array<String> = arrayOf<String>()
    )

    data class ESM_Type_Scale (
        var esm_scale_max: Int,
        var esm_scale_min: Int,
        var esm_scale_start: Int,
        var esm_scale_max_label: String,
        var esm_scale_min_label: String,
        var esm_scale_step: Int
    )



}